package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerAnalyticsOverviewDTO {
    private Long totalEvents;
    private Long totalTicketsSold;
    private BigDecimal totalRevenue;
    private Long upcomingEvents;
    private List<OrganizerTopEventDTO> topEvents;
    private Map<String, Long> ticketTypeDistribution;
    private List<MonthlyRevenueDTO> monthlySales;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizerTopEventDTO {
        private Long id;
        private String title;
        private LocalDateTime startDatetime;
        private Long ticketsSold;
        private BigDecimal revenue;
    }
}