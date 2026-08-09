package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSummaryDTO {
    private Long id;
    private String ticketCode;
    private String attendeeName;
    private String attendeeEmail;
    private String seatNumber;
    private TicketStatus status;
}
