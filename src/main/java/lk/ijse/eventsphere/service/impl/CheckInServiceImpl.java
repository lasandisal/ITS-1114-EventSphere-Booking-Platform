package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.CheckInResponseDTO;
import lk.ijse.eventsphere.dto.ScanRequestDTO;
import lk.ijse.eventsphere.entity.*;
import lk.ijse.eventsphere.enums.BookingStatus;
import lk.ijse.eventsphere.enums.TicketStatus;
import lk.ijse.eventsphere.exception.InvalidTicketException;
import lk.ijse.eventsphere.exception.TicketAlreadyUsedException;
import lk.ijse.eventsphere.repository.CheckInRepository;
import lk.ijse.eventsphere.repository.TicketRepository;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.CheckInService;
import lk.ijse.eventsphere.util.TicketSigningUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final TicketRepository ticketRepository;
    private final CheckInRepository checkInRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TicketSigningUtil ticketSigningUtil;

    @Override
    @Transactional
    public CheckInResponseDTO scanTicket(ScanRequestDTO request) {
        // Cheap check first, before touching the database — rejects a
        // garbage/forged payload without even locking a row.
        if (!ticketSigningUtil.verifySignedPayload(request.getSignedPayload())) {
            throw new InvalidTicketException("This QR code is invalid or has been tampered with");
        }
        String ticketCode = ticketSigningUtil.extractTicketCode(request.getSignedPayload());

        Ticket ticket = ticketRepository.lockByTicketCode(ticketCode)
                .orElseThrow(() -> new InvalidTicketException("No matching ticket found"));

        Booking booking = ticket.getBookingItem().getBooking();
        Event event = booking.getEvent();

        // Ownership: only the organizer who owns this event (or an admin)
        // can check attendees in for it — same pattern as event/ticket-type
        // ownership checks elsewhere.
        User staff = currentUserProvider.getCurrentUser();
        boolean isAdmin = staff.getRoles().stream()
                .map(r -> r.getName().name())
                .anyMatch(name -> name.equals("ADMIN"));
        if (!isAdmin && !event.getOrganizer().getUser().getId().equals(staff.getId())) {
            throw new AccessDeniedException("You cannot check in tickets for an event you do not own");
        }

        // A signed QR only ever gets generated for a CONFIRMED booking (see
        // PaymentServiceImpl.confirmPayment) — this should be unreachable in
        // practice, but it's cheap insurance against a stale/edge-case row.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidTicketException("This ticket is not linked to a confirmed booking");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new InvalidTicketException("This ticket has been cancelled");
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            // Still logged — CheckIn is an append-only audit trail of every
            // scan attempt, not just successful ones (see entity comment).
            checkInRepository.save(CheckIn.builder()
                    .ticket(ticket)
                    .checkedInBy(staff)
                    .locationNote(request.getLocationNote())
                    .build());
            throw new TicketAlreadyUsedException(
                    "This ticket was already used for entry (attendee: " + ticket.getAttendeeName() + ")");
        }

        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        CheckIn checkIn = checkInRepository.save(CheckIn.builder()
                .ticket(ticket)
                .checkedInBy(staff)
                .locationNote(request.getLocationNote())
                .build());

        return CheckInResponseDTO.builder()
                .ticketId(ticket.getId())
                .attendeeName(ticket.getAttendeeName())
                .seatNumber(ticket.getSeatNumber())
                .eventTitle(event.getTitle())
                .checkedInAt(checkIn.getCheckInTime() != null ? checkIn.getCheckInTime() : LocalDateTime.now())
                .build();
    }
}
