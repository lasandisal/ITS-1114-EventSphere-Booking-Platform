package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Booking;
import lk.ijse.eventsphere.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now);
}