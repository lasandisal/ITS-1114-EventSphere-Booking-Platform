package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.*;
import lk.ijse.eventsphere.entity.*;
import lk.ijse.eventsphere.enums.BookingStatus;
import lk.ijse.eventsphere.enums.EventStatus;
import lk.ijse.eventsphere.enums.OrganizerStatus;
import lk.ijse.eventsphere.exception.InsufficientInventoryException;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.*;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;

    @Value("${booking.hold-ttl-minutes}")
    private long holdTtlMinutes;

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingCreateRequestDTO request) {
        User user = currentUserProvider.getCurrentUser();

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("This event is not open for booking");
        }

        Booking booking = Booking.builder()
                .bookingReference(UUID.randomUUID().toString())
                .user(user)
                .event(event)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .expiresAt(LocalDateTime.now().plusMinutes(holdTtlMinutes))
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (BookingItemRequestDTO itemRequest : request.getItems()) {
            int quantity = itemRequest.getAttendees().size();

            // Row-level lock held for the rest of THIS transaction only —
            // read, validate, decrement and move on. Never do anything slow
            // (network calls, external APIs) while this lock is held, or
            // concurrent checkouts on the same ticket type will queue up.
            TicketType ticketType = ticketTypeRepository.lockById(itemRequest.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ticket type not found: " + itemRequest.getTicketTypeId()));

            if (!ticketType.getEvent().getId().equals(event.getId())) {
                throw new IllegalArgumentException(
                        "Ticket type " + ticketType.getId() + " does not belong to the specified event");
            }

            if (ticketType.getAvailableQuantity() < quantity) {
                throw new InsufficientInventoryException(
                        "Only " + ticketType.getAvailableQuantity() + " '" + ticketType.getName()
                                + "' tickets remain, " + quantity + " requested");
            }

            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() - quantity);
            ticketTypeRepository.save(ticketType);

            // Price snapshot — subtotal is fixed at booking time and never
            // recalculated from a possibly-changed ticketType.price later.
            BigDecimal unitPrice = ticketType.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            BookingItem bookingItem = BookingItem.builder()
                    .booking(booking)
                    .ticketType(ticketType)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            // Ticket rows (with attendee detail) are created now, at booking
            // time, since attendee-per-seat info is captured up front — but
            // ticketCode is a bare UUID here with no QR/email dispatch and
            // issued_at left null. The signed QR payload (UUID + HMAC) is
            // only generated once payment is CONFIRMED, so a PENDING booking's
            // tickets can't be used for check-in even though the rows exist.
            List<Ticket> tickets = new ArrayList<>();
            for (AttendeeDTO attendee : itemRequest.getAttendees()) {
                tickets.add(Ticket.builder()
                        .bookingItem(bookingItem)
                        .ticketCode(UUID.randomUUID().toString())
                        .attendeeName(attendee.getName())
                        .attendeeEmail(attendee.getEmail())
                        .seatNumber(attendee.getSeatNumber())
                        .build());
            }
            bookingItem.setTickets(tickets);
            booking.getItems().add(bookingItem);
        }

        booking.setTotalAmount(total);
        bookingRepository.save(booking); // cascades BookingItem + Ticket

        return toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId) {
        Booking booking = findOwnedBooking(bookingId);
        return toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getMyBookings(Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        return bookingRepository.findByUserId(user.getId(), pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public BookingResponseDTO cancelBooking(Long bookingId) {
        Booking booking = findOwnedBooking(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending booking can be cancelled this way — confirmed bookings need a refund workflow");
        }

        releaseInventory(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return toDto(booking);
    }

    @Override
    @Transactional
    public void expireStaleBookings() {
        List<Booking> stale = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING, LocalDateTime.now());

        for (Booking booking : stale) {
            releaseInventory(booking);
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
        }

        if (!stale.isEmpty()) {
            log.info("Expired {} stale PENDING booking(s) and released their held inventory", stale.size());
        }
    }

    @Override
    @Transactional
    public void releaseFailedPaymentBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Idempotent — a duplicate/late webhook re-delivering the same
        // failure must not double-release inventory that a prior delivery
        // (or the expiry job) already returned to the pool.
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }

        releaseInventory(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Released inventory for booking {} after payment failure", booking.getBookingReference());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getMyBookings(String tab, Pageable pageable) {
        User currentUser = currentUserProvider.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        Page<Booking> bookings;

        if ("past".equalsIgnoreCase(tab)) {
            bookings = bookingRepository.findPastBookings(currentUser.getId(), BookingStatus.CONFIRMED, now, pageable);
        } else if ("cancelled".equalsIgnoreCase(tab)) {
            List<BookingStatus> statuses = List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED);
            bookings = bookingRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(currentUser.getId(), statuses, pageable);
        } else { // "upcoming" default
            List<BookingStatus> statuses = List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);
            bookings = bookingRepository.findUpcomingBookings(currentUser.getId(), statuses, now, pageable);
        }

        return bookings.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByEventId(Long eventId) {
        return bookingRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    // ==================== helpers ====================

    // Shared by manual cancel and the expiry job — re-locks each ticket type
    // (a fresh lock, not the one from the original checkout transaction,
    // which is long since released) before adding the quantity back.
    private void releaseInventory(Booking booking) {
        for (BookingItem item : booking.getItems()) {
            TicketType ticketType = ticketTypeRepository.lockById(item.getTicketType().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ticket type not found: " + item.getTicketType().getId()));
            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + item.getQuantity());
            ticketTypeRepository.save(ticketType);
        }
    }

    /*
    * Standard findById() only loaded the Booking entity,
    * leaving items unloaded (LAZY).
    * When toDto() tried to read booking.getItems(),
    * the DB session was already closed, causing LazyInitializationException.
    * Using @Transactional(readOnly = true) and JOIN FETCH keeps the session open
    * and retrieves both Booking and its items in a single, optimized SQL query,
    * preventing 500 errors and N+1 query overhead.
    * */
    private Booking findOwnedBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithItems(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        User user = currentUserProvider.getCurrentUser();
        boolean isAdmin = user.getRoles().stream()
                .map(r -> r.getName().name())
                .anyMatch(name -> name.equals("ADMIN"));

        if (!isAdmin && !booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this booking");
        }
        return booking;
    }

    private BookingResponseDTO toDto(Booking booking) {
        User user = booking.getUser();
        Event event = booking.getEvent();

        List<BookingItemResponseDTO> itemDTOs = booking.getItems() != null
                ? booking.getItems().stream()
                .map(item -> BookingItemResponseDTO.builder()
                        .id(item.getId())
                        .ticketTypeId(item.getTicketType() != null ? item.getTicketType().getId() : null)
                        .ticketTypeName(item.getTicketType() != null ? item.getTicketType().getName() : null)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList()
                : List.of();

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .eventId(event != null ? event.getId() : null)
                .eventTitle(event != null ? event.getTitle() : null)
                .userId(user != null ? user.getId() : null)
                .customerName(user != null ? user.getFullName() : "N/A")
                .customerEmail(user != null ? user.getEmail() : "N/A")
                .customerPhone(user != null ? user.getPhone() : "N/A")
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .confirmedAt(booking.getConfirmedAt())
                .items(itemDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewDTO getDashboardOverview() {
        Long totalUsers = userRepository.count();
        Long totalOrganizers = (long) organizerRepository.findByStatus(OrganizerStatus.APPROVED).size();
        Long pendingOrganizers = (long) organizerRepository.findByStatus(OrganizerStatus.PENDING).size();
        Long publishedEvents = eventRepository.countByStatus(EventStatus.PUBLISHED);

        BigDecimal grossRevenue = bookingRepository.calculateTotalGrossRevenue(BookingStatus.CONFIRMED);
        Long ticketsSold = bookingRepository.calculateTotalTicketsSold(BookingStatus.CONFIRMED);

        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        // Group by Event
        Map<Event, List<Booking>> eventBookingsMap = confirmedBookings.stream()
                .filter(b -> b.getEvent() != null)
                .collect(Collectors.groupingBy(Booking::getEvent));

        List<TopEventDTO> topEvents = eventBookingsMap.entrySet().stream()
                .map(entry -> {
                    Event event = entry.getKey();
                    List<Booking> bookings = entry.getValue();

                    long eventTicketsSold = bookings.stream()
                            .filter(b -> b.getItems() != null)
                            .flatMap(b -> b.getItems().stream())
                            .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                            .sum();

                    BigDecimal eventRevenue = bookings.stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TopEventDTO.builder()
                            .eventId(event.getId())
                            .title(event.getTitle())
                            .organizerName(event.getOrganizer() != null ? event.getOrganizer().getBusinessName() : "N/A")
                            .categoryName(event.getCategory() != null ? event.getCategory().getName() : "General")
                            .ticketsSold(eventTicketsSold)
                            .totalRevenue(eventRevenue)
                            .build();
                })
                .sorted(Comparator.comparing(TopEventDTO::getTotalRevenue).reversed())
                .limit(5)
                .toList();

        // Group by Organizer Name
        Map<String, List<Booking>> organizerBookingsMap = confirmedBookings.stream()
                .filter(b -> b.getEvent() != null && b.getEvent().getOrganizer() != null)
                .collect(Collectors.groupingBy(b -> b.getEvent().getOrganizer().getBusinessName()));

        List<TopOrganizerDTO> topOrganizers = organizerBookingsMap.entrySet().stream()
                .map(entry -> {
                    String orgName = entry.getKey();
                    List<Booking> bookings = entry.getValue();

                    long distinctEvents = bookings.stream()
                            .map(b -> b.getEvent().getId())
                            .distinct()
                            .count();

                    long orgTicketsSold = bookings.stream()
                            .filter(b -> b.getItems() != null)
                            .flatMap(b -> b.getItems().stream())
                            .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                            .sum();

                    BigDecimal orgRevenue = bookings.stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TopOrganizerDTO.builder()
                            .organizerName(orgName)
                            .eventsCount(distinctEvents)
                            .ticketsSold(orgTicketsSold)
                            .totalRevenue(orgRevenue)
                            .build();
                })
                .sorted(Comparator.comparing(TopOrganizerDTO::getTotalRevenue).reversed())
                .limit(5)
                .toList();

        return AdminAnalyticsOverviewDTO.builder()
                .totalUsers(totalUsers)
                .totalOrganizers(totalOrganizers)
                .pendingOrganizersCount(pendingOrganizers)
                .totalPublishedEvents(publishedEvents)
                .totalTicketsSold(ticketsSold != null ? ticketsSold : 0L)
                .totalGrossRevenue(grossRevenue != null ? grossRevenue : BigDecimal.ZERO)
                .topPerformingEvents(topEvents)
                .topOrganizers(topOrganizers)
                .build();
    }
}
