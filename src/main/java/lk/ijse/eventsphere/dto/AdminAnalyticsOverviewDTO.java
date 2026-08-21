package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsOverviewDTO {
    private Long totalUsers;
    private Long totalOrganizers;
    private Long pendingOrganizersCount;
    private Long totalPublishedEvents;
    private Long totalTicketsSold;
    private BigDecimal totalGrossRevenue;
    private List<TopEventDTO> topPerformingEvents;
    private List<TopOrganizerDTO> topOrganizers;
    private List<MonthlyRevenueDTO> monthlyRevenue; // Added for chart rendering
}