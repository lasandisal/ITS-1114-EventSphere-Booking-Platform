package lk.ijse.eventsphere.repository;

import jakarta.persistence.LockModeType;
import lk.ijse.eventsphere.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByEventId(Long eventId);

    // Row-level lock for checkout: call this — never a plain findById — before
    // reading/decrementing available_quantity. Must run inside a single
    // @Transactional service method so the lock is held for the whole
    // read-modify-write, and the transaction should be kept short (this row
    // is contended under concurrent checkout on popular events).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select t from TicketType t where t.id = :id")
    Optional<TicketType> lockById(@Param("id") Long id);
}
