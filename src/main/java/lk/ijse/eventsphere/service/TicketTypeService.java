package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.TicketTypeRequestDTO;
import lk.ijse.eventsphere.dto.TicketTypeResponseDTO;

import java.util.List;

public interface TicketTypeService {

    TicketTypeResponseDTO addTicketType(Long eventId, TicketTypeRequestDTO request);

    TicketTypeResponseDTO updateTicketType(Long ticketTypeId, TicketTypeRequestDTO request);

    List<TicketTypeResponseDTO> getTicketTypesForEvent(Long eventId);
}
