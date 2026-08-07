package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// one entry per attendee/seat within a booking — a booking can mix multiple of these
// across different ticket types in a single request
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequestDTO {

    @NotNull
    private Long ticketTypeId;

    @NotBlank
    private String attendeeName;

    @NotBlank @Email
    private String attendeeEmail;

    private String seatNumber; // optional — only for seated events
}