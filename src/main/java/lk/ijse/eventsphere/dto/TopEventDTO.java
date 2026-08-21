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
public class TopEventDTO {
    private Long eventId;
    private String title;
    private String organizerName;
    private String categoryName;
    private Long ticketsSold;
    private BigDecimal totalRevenue;
}