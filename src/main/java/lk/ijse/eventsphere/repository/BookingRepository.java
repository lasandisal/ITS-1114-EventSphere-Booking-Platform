package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Booking;
import lk.ijse.eventsphere.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.items i " +
            "LEFT JOIN FETCH i.ticketType " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithItems(@Param("id") Long id);

    // 1. Upcoming: Confirmed or Pending bookings where event starts now or in future
    @Query("""
        SELECT b FROM Booking b 
        JOIN b.event e 
        WHERE b.user.id = :userId 
          AND b.status IN :statuses 
          AND e.startDatetime >= :now 
        ORDER BY e.startDatetime ASC
    """)
    Page<Booking> findUpcomingBookings(
            @Param("userId") Long userId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // 2. Past: Confirmed bookings where event already happened
    @Query("""
        SELECT b FROM Booking b 
        JOIN b.event e 
        WHERE b.user.id = :userId 
          AND b.status = :status 
          AND e.startDatetime < :now 
        ORDER BY e.startDatetime DESC
    """)
    Page<Booking> findPastBookings(
            @Param("userId") Long userId,
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // 3. Cancelled: Cancelled or Expired bookings
    Page<Booking> findByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            List<BookingStatus> statuses,
            Pageable pageable);
}