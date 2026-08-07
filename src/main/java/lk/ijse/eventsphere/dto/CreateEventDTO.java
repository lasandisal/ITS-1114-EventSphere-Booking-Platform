package lk.ijse.eventsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateEventDTO {

    @NotBlank
    private String title;

    private String description;

    private String category;

    @NotBlank
    private String venue;

    @NotNull @Future(message = "Event start must be in the future")
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    // an event needs at least one ticket type at creation time — organizers can add
    // more later via a separate "add ticket type" call
    @NotEmpty
    private List<@Valid CreateTicketTypeDTO> ticketTypes;
}
