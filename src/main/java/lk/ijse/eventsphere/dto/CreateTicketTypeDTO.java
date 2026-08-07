package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketTypeDTO {

    @NotBlank
    private String name; // e.g. "General", "VIP"

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    @Min(value = 1, message = "Must offer at least 1 ticket")
    private int totalQuantity;
}
