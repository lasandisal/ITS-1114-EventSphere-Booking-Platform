package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.CreateEventDTO;
import lk.ijse.eventsphere.dto.CreateTicketTypeDTO;
import lk.ijse.eventsphere.dto.EventDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {

    EventDTO createEvent(Long organizerId, CreateEventDTO request);

    EventDTO updateEvent(Long eventId, Long organizerId, CreateEventDTO request);

    EventDTO publishEvent(Long eventId, Long organizerId);

    EventDTO cancelEvent(Long eventId, Long organizerId);

    // soft delete — only permitted while the event has no bookings (checked in impl)
    void deleteEvent(Long eventId, Long organizerId);

    EventDTO addTicketType(Long eventId, Long organizerId, CreateTicketTypeDTO request);

    // organizer's own management list — includes DRAFT/CANCELLED
    Page<EventDTO> getMyEvents(Long organizerId, Pageable pageable);

    // attendee-facing discovery — backs the AI assistant's search_events tool; PUBLISHED only
    Page<EventDTO> searchEvents(String keyword, String category, Pageable pageable);

    // attendee-facing detail lookup — backs the AI assistant's get_event_details tool; PUBLISHED only
    EventDTO getPublishedEventDetails(Long eventId);
}
