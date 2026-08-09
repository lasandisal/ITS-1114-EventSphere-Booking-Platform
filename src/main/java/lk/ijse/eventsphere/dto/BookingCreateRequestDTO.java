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
public class BookingCreateRequestDTO {

    @NotNull(message = "Event is required")
    private Long eventId;

    @NotEmpty(message = "At least one booking item is required")
    @Valid
    private List<BookingItemRequestDTO> items;
}
