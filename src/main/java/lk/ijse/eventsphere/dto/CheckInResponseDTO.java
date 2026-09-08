package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponseDTO {
    private Long ticketId;
    private String attendeeName;
    private String attendeeEmail;
    private String seatNumber;
    private String eventTitle;
    private String ticketTypeName;
    private String bookingReference;
    private LocalDateTime checkedInAt;
}
