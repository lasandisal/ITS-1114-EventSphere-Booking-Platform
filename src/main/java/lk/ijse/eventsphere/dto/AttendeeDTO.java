package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.Email;
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
public class AttendeeDTO {

    @NotBlank(message = "Attendee name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Attendee email is required")
    @Email(message = "Attendee email must be a valid address")
    private String email;

    @Size(max = 20)
    private String seatNumber;
}
