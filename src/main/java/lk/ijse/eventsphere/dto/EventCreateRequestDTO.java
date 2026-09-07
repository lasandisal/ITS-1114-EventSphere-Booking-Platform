package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateRequestDTO {

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Venue is required")
    private Long venueId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 500, message = "Banner URL cannot exceed 500 characters")
    private String bannerUrl;

    @NotNull(message = "Start date/time is required")
    @Future(message = "Start date/time must be in the future")
    private LocalDateTime startDatetime;

    @NotNull(message = "End date/time is required")
    private LocalDateTime endDatetime;
}
