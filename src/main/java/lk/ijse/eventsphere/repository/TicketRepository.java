package lk.ijse.eventsphere.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import lk.ijse.eventsphere.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    // Row-level lock for check-in: two staff scanning the same QR (e.g. a
    // screenshot shared between two people) at nearly the same instant must
    // not both succeed — the second scan blocks until the first commits,
    // then sees status=USED and gets TicketAlreadyUsedException instead of
    // granting a second entry.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select t from Ticket t where t.ticketCode = :ticketCode")
    Optional<Ticket> lockByTicketCode(@Param("ticketCode") String ticketCode);

    List<Ticket> findByBookingItemId(Long bookingItemId);
}
