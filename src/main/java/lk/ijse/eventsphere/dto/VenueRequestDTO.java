package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VenueRequestDTO {

    @NotBlank(message = "Venue name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String addressLine;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal longitude;
}
