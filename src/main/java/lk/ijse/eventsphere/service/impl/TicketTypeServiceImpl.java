package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.TicketTypeRequestDTO;
import lk.ijse.eventsphere.dto.TicketTypeResponseDTO;
import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.entity.TicketType;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.EventRepository;
import lk.ijse.eventsphere.repository.TicketTypeRepository;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public TicketTypeResponseDTO addTicketType(Long eventId, TicketTypeRequestDTO request) {
        Event event = findEventOwnedByCurrentOrganizer(eventId);

        TicketType ticketType = TicketType.builder()
                .event(event)
                .name(request.getName())
                .price(request.getPrice())
                .totalQuantity(request.getTotalQuantity())
                // available_quantity starts equal to total_quantity — this is
                // the field checkout will later lock and decrement.
                .availableQuantity(request.getTotalQuantity())
                .saleStart(request.getSaleStart())
                .saleEnd(request.getSaleEnd())
                .build();

        ticketTypeRepository.save(ticketType);
        return toDto(ticketType);
    }

    @Override
    @Transactional
    public TicketTypeResponseDTO updateTicketType(Long ticketTypeId, TicketTypeRequestDTO request) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));

        // Ownership is checked via the parent event, not the ticket type itself.
        findEventOwnedByCurrentOrganizer(ticketType.getEvent().getId());

        // Shrinking total_quantity below what's already sold would corrupt
        // available_quantity — guard against that explicitly rather than
        // letting it silently go negative.
        int sold = ticketType.getTotalQuantity() - ticketType.getAvailableQuantity();
        if (request.getTotalQuantity() < sold) {
            throw new IllegalArgumentException(
                    "Cannot reduce total quantity below " + sold + " (already sold)");
        }

        int delta = request.getTotalQuantity() - ticketType.getTotalQuantity();
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());
        ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + delta);
        ticketType.setSaleStart(request.getSaleStart());
        ticketType.setSaleEnd(request.getSaleEnd());

        ticketTypeRepository.save(ticketType);
        return toDto(ticketType);
    }

    @Override
    public List<TicketTypeResponseDTO> getTicketTypesForEvent(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    private Event findEventOwnedByCurrentOrganizer(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        User user = currentUserProvider.getCurrentUser();
        boolean isAdmin = user.getRoles().stream()
                .map(r -> r.getName().name())
                .anyMatch(name -> name.equals("ADMIN"));

        if (!isAdmin && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this event");
        }
        return event;
    }

    private TicketTypeResponseDTO toDto(TicketType tt) {
        return TicketTypeResponseDTO.builder()
                .id(tt.getId())
                .eventId(tt.getEvent().getId())
                .name(tt.getName())
                .price(tt.getPrice())
                .totalQuantity(tt.getTotalQuantity())
                .availableQuantity(tt.getAvailableQuantity())
                .saleStart(tt.getSaleStart())
                .saleEnd(tt.getSaleEnd())
                .build();
    }
}
