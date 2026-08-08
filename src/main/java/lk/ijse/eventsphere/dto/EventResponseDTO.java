package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDTO {
    private Long id;
    private Long organizerId;
    private String organizerName;
    private Long categoryId;
    private String categoryName;
    private Long venueId;
    private String venueName;
    private String title;
    private String description;
    private String bannerUrl;
    private EventStatus status;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private List<TicketTypeResponseDTO> ticketTypes;
}
