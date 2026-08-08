package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Booking;
import lk.ijse.eventsphere.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    // Backs the AI assistant's get_my_bookings() tool and the user's booking
    // history screen.
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByEventId(Long eventId, Pageable pageable);

    // Used by the scheduled expiry job: any PENDING booking whose hold has
    // lapsed gets cancelled and its locked inventory released.
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime cutoff);
}
