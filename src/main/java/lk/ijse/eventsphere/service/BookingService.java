package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.BookingRequestDTO;
import lk.ijse.eventsphere.dto.BookingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    // core transactional flow: pessimistic-locks each ticket type, validates availability,
    // decrements inventory, creates the booking + per-attendee tickets. See impl for detail.
    BookingResponseDTO createBooking(Long userId, BookingRequestDTO request);

    // backs the AI assistant's get_my_bookings tool and the attendee's own booking history
    Page<BookingResponseDTO> getMyBookings(Long userId, Pageable pageable);

    // organizer's bookings/attendee view for one of their events
    Page<BookingResponseDTO> getBookingsForEvent(Long eventId, Long organizerId, Pageable pageable);

    // releases the held inventory back to the ticket type(s)
    BookingResponseDTO cancelBooking(Long bookingId, Long userId);

    // invoked by a scheduled job; sweeps PENDING bookings past their 10-minute hold,
    // marks them EXPIRED, and restocks inventory. No-op today since bookings currently
    // confirm instantly, but the plumbing is ready for the payment-gateway integration.
    void expireStaleBookings();
}
