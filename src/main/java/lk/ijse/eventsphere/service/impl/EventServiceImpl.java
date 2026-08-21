package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.EventCreateRequestDTO;
import lk.ijse.eventsphere.dto.EventResponseDTO;
import lk.ijse.eventsphere.dto.EventUpdateRequestDTO;
import lk.ijse.eventsphere.dto.TicketTypeResponseDTO;
import lk.ijse.eventsphere.entity.*;
import lk.ijse.eventsphere.enums.EventStatus;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.*;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final OrganizerRepository organizerRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public EventResponseDTO createEvent(EventCreateRequestDTO request) {
        Organizer organizer = currentOrganizer();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.getVenueId()));

        if (!request.getEndDatetime().isAfter(request.getStartDatetime())) {
            throw new IllegalArgumentException("End date/time must be after start date/time");
        }

        Event event = Event.builder()
                .organizer(organizer)
                .category(category)
                .venue(venue)
                .title(request.getTitle())
                .description(request.getDescription())
                .bannerUrl(request.getBannerUrl())
                .status(EventStatus.DRAFT)
                .startDatetime(request.getStartDatetime())
                .endDatetime(request.getEndDatetime())
                .build();

        eventRepository.save(event);
        return toDto(event, List.of());
    }

    @Override
    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateRequestDTO request) {
        Event event = findEventOwnedByCurrentOrganizer(eventId);

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getBannerUrl() != null) event.setBannerUrl(request.getBannerUrl());
        if (request.getStartDatetime() != null) event.setStartDatetime(request.getStartDatetime());
        if (request.getEndDatetime() != null) event.setEndDatetime(request.getEndDatetime());
        if (request.getCategoryId() != null) {
            event.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId())));
        }
        if (request.getVenueId() != null) {
            event.setVenue(venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.getVenueId())));
        }

        if (!event.getEndDatetime().isAfter(event.getStartDatetime())) {
            throw new IllegalArgumentException("End date/time must be after start date/time");
        }

        eventRepository.save(event);
        return toDto(event, mapTicketTypes(event.getId()));
    }

    @Override
    @Transactional
    public EventResponseDTO publishEvent(Long eventId) {
        Event event = findEventOwnedByCurrentOrganizer(eventId);

        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(eventId);
        if (ticketTypes.isEmpty()) {
            throw new IllegalArgumentException("Cannot publish an event with no ticket types defined");
        }

        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        return toDto(event, mapTicketTypes(eventId));
    }

    @Override
    @Transactional
    public EventResponseDTO cancelEvent(Long eventId) {
        Event event = findEventOwnedByCurrentOrganizer(eventId);
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
        return toDto(event, mapTicketTypes(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        return toDto(event, mapTicketTypes(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> searchPublishedEvents(String keyword, Long categoryId, Pageable pageable) {
        String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Event> eventPage = eventRepository.searchPublishedEvents(
                EventStatus.PUBLISHED,
                sanitizedKeyword,
                categoryId,
                pageable
        );

        return eventPage.map(event -> toDto(event, mapTicketTypes(event.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getMyEvents(Pageable pageable) {
        Organizer organizer = currentOrganizer();
        return eventRepository.findByOrganizerId(organizer.getId(), pageable)
                .map(event -> toDto(event, mapTicketTypes(event.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEventsAdmin(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(event -> toDto(event, mapTicketTypes(event.getId())));
    }

    // ==================== helpers ====================

    private Organizer currentOrganizer() {
        User user = currentUserProvider.getCurrentUser();
        return organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No organizer profile for this account — apply as an organizer first"));
    }

    private Event findEventOwnedByCurrentOrganizer(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        User user = currentUserProvider.getCurrentUser();
        boolean isAdmin = user.getRoles().stream()
                .map(r -> r.getName().name())
                .anyMatch(name -> name.equals("ADMIN"));

        if (!isAdmin && (event.getOrganizer() == null || !event.getOrganizer().getUser().getId().equals(user.getId()))) {
            throw new AccessDeniedException("You do not own this event");
        }
        return event;
    }

    private List<TicketTypeResponseDTO> mapTicketTypes(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(tt -> TicketTypeResponseDTO.builder()
                        .id(tt.getId())
                        .eventId(eventId)
                        .name(tt.getName())
                        .price(tt.getPrice())
                        .totalQuantity(tt.getTotalQuantity())
                        .availableQuantity(tt.getAvailableQuantity())
                        .saleStart(tt.getSaleStart())
                        .saleEnd(tt.getSaleEnd())
                        .build())
                .toList();
    }

    private EventResponseDTO toDto(Event event, List<TicketTypeResponseDTO> ticketTypes) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .organizerId(event.getOrganizer() != null ? event.getOrganizer().getId() : null)
                .organizerName(event.getOrganizer() != null ? event.getOrganizer().getBusinessName() : "N/A")
                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                .categoryName(event.getCategory() != null ? event.getCategory().getName() : "General")
                .venueId(event.getVenue() != null ? event.getVenue().getId() : null)
                .venueName(event.getVenue() != null ? event.getVenue().getName() : "TBD")
                .title(event.getTitle())
                .description(event.getDescription())
                .bannerUrl(event.getBannerUrl())
                .status(event.getStatus())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .ticketTypes(ticketTypes)
                .build();
    }
}