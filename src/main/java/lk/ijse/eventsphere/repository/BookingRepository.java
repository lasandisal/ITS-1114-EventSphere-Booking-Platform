package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Booking;
import lk.ijse.eventsphere.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // backs the AI assistant's get_my_bookings() tool and the attendee's "my bookings" screen
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    // organizer viewing bookings/attendees for one of their events
    Page<Booking> findByEventId(Long eventId, Pageable pageable);

    // picked up by the scheduled job that auto-expires PENDING holds (10-min window) and
    // releases the reserved inventory back to the ticket type
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime cutoff);
}