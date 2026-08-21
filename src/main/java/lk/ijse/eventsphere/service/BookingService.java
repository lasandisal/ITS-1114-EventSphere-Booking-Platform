package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.AdminAnalyticsOverviewDTO;
import lk.ijse.eventsphere.dto.BookingCreateRequestDTO;
import lk.ijse.eventsphere.dto.BookingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {

    // Phase 1 of the 2-phase flow: locks inventory, creates a PENDING
    // booking with an expiry hold. Payment confirmation (phase 2) is a
    // separate service that transitions PENDING -> CONFIRMED.
    BookingResponseDTO createBooking(BookingCreateRequestDTO request);

    BookingResponseDTO getBookingById(Long bookingId);

    // Backs the AI assistant's get_my_bookings() tool and the booking
    // history screen.
    Page<BookingResponseDTO> getMyBookings(Pageable pageable);

    // User-initiated early release of a PENDING hold (doesn't wait for TTL).
    BookingResponseDTO cancelBooking(Long bookingId);

    // Called by the scheduled expiry job — releases inventory for every
    // PENDING booking whose hold has lapsed.
    void expireStaleBookings();

    // Called from the PayHere webhook handler when a payment fails/is
    // cancelled/is charged back — releases inventory immediately rather than
    // waiting out the remainder of the hold TTL.
    void releaseFailedPaymentBooking(Long bookingId);

    Page<BookingResponseDTO> getMyBookings(String tab, Pageable pageable);

    List<BookingResponseDTO> getBookingsByEventId(Long eventId);
    AdminAnalyticsOverviewDTO getDashboardOverview();

}
