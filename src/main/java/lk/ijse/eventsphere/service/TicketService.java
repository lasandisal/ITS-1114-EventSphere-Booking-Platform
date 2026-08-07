package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.TicketDTO;

import java.util.List;

public interface TicketService {

    // venue check-in: organizer scans a ticket's code; validates it belongs to one of
    // their events, is CONFIRMED/VALID, and not already checked in
    TicketDTO checkIn(String ticketCode, Long organizerId);

    // full attendee list for one of the organizer's events (name/email/seat/status)
    List<TicketDTO> getAttendeesForEvent(Long eventId, Long organizerId);
}
