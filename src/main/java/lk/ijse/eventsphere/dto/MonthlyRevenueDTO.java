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
public class MonthlyRevenueDTO {
    private String month;       // e.g. "Mar", "Apr", "Aug"
    private String key;         // e.g. "2026-03", "2026-08"
    private BigDecimal revenue;
    private Long ticketsSold;
}