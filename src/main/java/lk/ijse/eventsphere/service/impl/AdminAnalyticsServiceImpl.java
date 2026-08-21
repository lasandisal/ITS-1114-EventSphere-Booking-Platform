package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.AdminAnalyticsOverviewDTO;
import lk.ijse.eventsphere.dto.MonthlyRevenueDTO;
import lk.ijse.eventsphere.dto.TopEventDTO;
import lk.ijse.eventsphere.dto.TopOrganizerDTO;
import lk.ijse.eventsphere.entity.Booking;
import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.enums.BookingStatus;
import lk.ijse.eventsphere.enums.EventStatus;
import lk.ijse.eventsphere.enums.OrganizerStatus;
import lk.ijse.eventsphere.repository.BookingRepository;
import lk.ijse.eventsphere.repository.EventRepository;
import lk.ijse.eventsphere.repository.OrganizerRepository;
import lk.ijse.eventsphere.repository.UserRepository;
import lk.ijse.eventsphere.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

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

        // 1. Top Performing Events
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

                    BigDecimal eventRev = bookings.stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TopEventDTO.builder()
                            .eventId(event.getId())
                            .title(event.getTitle())
                            .organizerName(event.getOrganizer() != null ? event.getOrganizer().getBusinessName() : "N/A")
                            .categoryName(event.getCategory() != null ? event.getCategory().getName() : "General")
                            .ticketsSold(eventTicketsSold)
                            .totalRevenue(eventRev)
                            .build();
                })
                .sorted(Comparator.comparing(TopEventDTO::getTotalRevenue).reversed())
                .limit(5)
                .toList();

        // 2. Top Organizers
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

                    BigDecimal orgRev = bookings.stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TopOrganizerDTO.builder()
                            .organizerName(orgName)
                            .eventsCount(distinctEvents)
                            .ticketsSold(orgTicketsSold)
                            .totalRevenue(orgRev)
                            .build();
                })
                .sorted(Comparator.comparing(TopOrganizerDTO::getTotalRevenue).reversed())
                .limit(5)
                .toList();

        // 3. Last 6 Months Revenue Breakdown for Chart
        List<MonthlyRevenueDTO> monthlyRevenue = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            YearMonth targetMonth = currentYearMonth.minusMonths(i);
            String monthKey = targetMonth.format(keyFormatter);
            String monthLabel = targetMonth.format(labelFormatter);

            List<Booking> monthBookings = confirmedBookings.stream()
                    .filter(b -> b.getCreatedAt() != null && YearMonth.from(b.getCreatedAt()).equals(targetMonth))
                    .toList();

            BigDecimal monthRev = monthBookings.stream()
                    .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long monthTickets = monthBookings.stream()
                    .filter(b -> b.getItems() != null)
                    .flatMap(b -> b.getItems().stream())
                    .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();

            monthlyRevenue.add(MonthlyRevenueDTO.builder()
                    .month(monthLabel)
                    .key(monthKey)
                    .revenue(monthRev)
                    .ticketsSold(monthTickets)
                    .build());
        }

        return AdminAnalyticsOverviewDTO.builder()
                .totalUsers(totalUsers)
                .totalOrganizers(totalOrganizers)
                .pendingOrganizersCount(pendingOrganizers)
                .totalPublishedEvents(publishedEvents)
                .totalTicketsSold(ticketsSold != null ? ticketsSold : 0L)
                .totalGrossRevenue(grossRevenue != null ? grossRevenue : BigDecimal.ZERO)
                .topPerformingEvents(topEvents)
                .topOrganizers(topOrganizers)
                .monthlyRevenue(monthlyRevenue)
                .build();
    }
}