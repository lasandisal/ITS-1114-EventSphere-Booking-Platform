package lk.ijse.eventsphere.dto;

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
public class TopOrganizerDTO {
    private String organizerName;
    private Long eventsCount;
    private Long ticketsSold;
    private BigDecimal totalRevenue;
}