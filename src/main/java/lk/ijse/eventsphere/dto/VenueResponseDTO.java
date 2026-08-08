package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueResponseDTO {
    private Long id;
    private String name;
    private String addressLine;
    private String city;
    private Integer capacity;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
