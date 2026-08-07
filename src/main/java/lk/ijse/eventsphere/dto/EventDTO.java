package lk.ijse.eventsphere.dto;
import lk.ijse.eventsphere.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String venue;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private EventStatus status;
    private Long organizerId;
    private String organizerName;
    private List<TicketTypeDTO> ticketTypes;
    private LocalDateTime createdAt;
}
