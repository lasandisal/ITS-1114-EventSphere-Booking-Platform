package lk.ijse.eventsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {

    @NotNull
    private Long eventId;

    // one or more tickets, potentially spanning several ticket types, booked atomically
    @NotEmpty
    private List<@Valid TicketRequestDTO> tickets;
}
