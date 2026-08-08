package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Used by the check-in scan flow, after the HMAC signature on the QR
    // payload has already been verified locally.
    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByBookingItemId(Long bookingItemId);
}
