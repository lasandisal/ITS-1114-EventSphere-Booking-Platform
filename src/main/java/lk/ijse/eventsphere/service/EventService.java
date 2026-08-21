package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.EventCreateRequestDTO;
import lk.ijse.eventsphere.dto.EventResponseDTO;
import lk.ijse.eventsphere.dto.EventUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {

    EventResponseDTO createEvent(EventCreateRequestDTO request);

    EventResponseDTO updateEvent(Long eventId, EventUpdateRequestDTO request);

    EventResponseDTO publishEvent(Long eventId);

    EventResponseDTO cancelEvent(Long eventId);

    EventResponseDTO getEventById(Long eventId);

    // Public discovery — PUBLISHED events only. Backs both the browse UI and
    // the AI assistant's search_events(keyword, category) tool.
    Page<EventResponseDTO> searchPublishedEvents(String keyword, Long categoryId, Pageable pageable);

    // Organizer's own dashboard — all statuses, own events only.
    Page<EventResponseDTO> getMyEvents(Pageable pageable);

    Page<EventResponseDTO> getAllEventsAdmin(Pageable pageable);
}
