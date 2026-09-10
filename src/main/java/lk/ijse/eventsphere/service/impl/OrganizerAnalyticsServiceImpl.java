package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.MonthlyRevenueDTO;
import lk.ijse.eventsphere.dto.OrganizerAnalyticsOverviewDTO;
import lk.ijse.eventsphere.entity.Booking;
import lk.ijse.eventsphere.entity.BookingItem;
import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.entity.Organizer;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.BookingStatus;
import lk.ijse.eventsphere.enums.EventStatus;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.BookingRepository;
import lk.ijse.eventsphere.repository.EventRepository;
import lk.ijse.eventsphere.repository.OrganizerRepository;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.OrganizerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizerAnalyticsServiceImpl implements OrganizerAnalyticsService {

    private final CurrentUserProvider currentUserProvider;
    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizerAnalyticsOverviewDTO getOverview() {
        User user = currentUserProvider.getCurrentUser();
        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No organizer profile for this account - apply as an organizer first"));

        List<Event> events = eventRepository.findByOrganizerId(organizer.getId());
        long totalEvents = events.size();

        LocalDateTime now = LocalDateTime.now();
        long upcomingEvents = events.stream()
                .filter(e -> e.getStatus() != EventStatus.CANCELLED && e.getStartDatetime() != null && e.getStartDatetime().isAfter(now))
                .count();

        List<Booking> bookings = bookingRepository.findByEventOrganizerId(organizer.getId());
        List<Booking> confirmedBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        long totalTicketsSold = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, Long> ticketTypeDistribution = new HashMap<>();

        for (Booking b : confirmedBookings) {
            if (b.getTotalAmount() != null) {
                totalRevenue = totalRevenue.add(b.getTotalAmount());
            }
            if (b.getItems() != null) {
                for (BookingItem item : b.getItems()) {
                    long qty = item.getQuantity() != null ? item.getQuantity() : 0L;
                    totalTicketsSold += qty;
                    String typeName = (item.getTicketType() != null && item.getTicketType().getName() != null)
                            ? item.getTicketType().getName()
                            : "General";
                    ticketTypeDistribution.put(typeName, ticketTypeDistribution.getOrDefault(typeName, 0L) + qty);
                }
            }
        }

        // Top Events
        Map<Long, List<Booking>> bookingsByEvent = confirmedBookings.stream()
                .filter(b -> b.getEvent() != null)
                .collect(Collectors.groupingBy(b -> b.getEvent().getId()));

        List<OrganizerAnalyticsOverviewDTO.OrganizerTopEventDTO> topEvents = events.stream()
                .map(ev -> {
                    List<Booking> evBookings = bookingsByEvent.getOrDefault(ev.getId(), List.of());
                    long tix = 0L;
                    BigDecimal rev = BigDecimal.ZERO;
                    for (Booking b : evBookings) {
                        if (b.getTotalAmount() != null) rev = rev.add(b.getTotalAmount());
                        if (b.getItems() != null) {
                            for (BookingItem item : b.getItems()) {
                                if (item.getQuantity() != null) tix += item.getQuantity();
                            }
                        }
                    }
                    return OrganizerAnalyticsOverviewDTO.OrganizerTopEventDTO.builder()
                            .id(ev.getId())
                            .title(ev.getTitle())
                            .startDatetime(ev.getStartDatetime())
                            .ticketsSold(tix)
                            .revenue(rev)
                            .build();
                })
                .sorted((a, b) -> {
                    int c = b.getRevenue().compareTo(a.getRevenue());
                    return c != 0 ? c : Long.compare(b.getTicketsSold(), a.getTicketsSold());
                })
                .limit(5)
                .toList();

        // 6-Month rolling sales
        List<MonthlyRevenueDTO> monthlySales = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth targetMonth = currentYearMonth.minusMonths(i);
            String monthLabel = targetMonth.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));
            String monthKey = targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            BigDecimal monthRevenue = BigDecimal.ZERO;
            long monthTickets = 0L;

            for (Booking b : confirmedBookings) {
                LocalDateTime bookingDate = b.getConfirmedAt() != null ? b.getConfirmedAt() : b.getCreatedAt();
                if (bookingDate != null) {
                    YearMonth bookingMonth = YearMonth.from(bookingDate);
                    if (bookingMonth.equals(targetMonth)) {
                        if (b.getTotalAmount() != null) monthRevenue = monthRevenue.add(b.getTotalAmount());
                        if (b.getItems() != null) {
                            for (BookingItem item : b.getItems()) {
                                if (item.getQuantity() != null) monthTickets += item.getQuantity();
                            }
                        }
                    }
                }
            }

            monthlySales.add(MonthlyRevenueDTO.builder()
                    .month(monthLabel)
                    .key(monthKey)
                    .revenue(monthRevenue)
                    .ticketsSold(monthTickets)
                    .build());
        }

        return OrganizerAnalyticsOverviewDTO.builder()
                .totalEvents(totalEvents)
                .totalTicketsSold(totalTicketsSold)
                .totalRevenue(totalRevenue)
                .upcomingEvents(upcomingEvents)
                .topEvents(topEvents)
                .ticketTypeDistribution(ticketTypeDistribution)
                .monthlySales(monthlySales)
                .build();
    }
}