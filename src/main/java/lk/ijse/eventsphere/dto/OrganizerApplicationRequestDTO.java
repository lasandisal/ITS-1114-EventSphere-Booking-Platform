package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerApplicationRequestDTO {

    @NotBlank(message = "Business name is required")
    @Size(max = 150)
    private String businessName;

    @Size(max = 2000)
    private String bio;
}
