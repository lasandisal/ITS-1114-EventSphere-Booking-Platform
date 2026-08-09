package lk.ijse.eventsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingItemRequestDTO {

    @NotNull(message = "Ticket type is required")
    private Long ticketTypeId;

    // quantity is implicit = attendees.size() — one attendee record per
    // ticket, matching the "individual attendee/seat per ticket" requirement
    // rather than a bare quantity number.
    @NotEmpty(message = "At least one attendee is required per ticket type")
    @Valid
    private List<AttendeeDTO> attendees;
}
