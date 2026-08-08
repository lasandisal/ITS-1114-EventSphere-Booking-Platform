package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// All fields optional — the service only applies the ones that are non-null,
// so a client can PATCH a single field (e.g. just the description) without
// resending the whole event.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdateRequestDTO {

    private Long categoryId;
    private Long venueId;

    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    private String bannerUrl;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
