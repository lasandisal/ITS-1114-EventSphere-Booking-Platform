package lk.ijse.eventsphere.repository;

import jakarta.persistence.LockModeType;
import lk.ijse.eventsphere.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    // shown on the event detail page (ticket tiers + live availability)
    List<TicketType> findByEventId(Long eventId);

    // row-level lock (SELECT ... FOR UPDATE) taken inside the booking transaction so two
    // concurrent bookings can't both read the same availableQuantity and oversell the
    // last few seats. Must only be called within a @Transactional service method.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findByIdForUpdate(@Param("id") Long id);
}