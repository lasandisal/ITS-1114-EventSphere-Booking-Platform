package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // venue check-in: organizer scans the ticket's QR/barcode, backend looks it up by code
    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByBookingId(Long bookingId);

    // organizer's full attendee list for an event (name/email/seat/status per ticket),
    // joined through ticketType -> event
    List<Ticket> findByTicketTypeEventId(Long eventId);
}