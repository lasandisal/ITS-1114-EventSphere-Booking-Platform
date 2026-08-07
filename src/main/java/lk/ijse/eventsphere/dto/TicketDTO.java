package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDTO {
    private Long id;
    private String ticketTypeName;
    private String attendeeName;
    private String attendeeEmail;
    private String seatNumber;
    private String ticketCode;
    private TicketStatus status;
}
