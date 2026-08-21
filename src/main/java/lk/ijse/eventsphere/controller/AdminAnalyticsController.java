package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.AdminAnalyticsOverviewDTO;
import lk.ijse.eventsphere.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<CommonResponse<AdminAnalyticsOverviewDTO>> getOverview() {
        AdminAnalyticsOverviewDTO overview = analyticsService.getDashboardOverview();
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Platform analytics retrieved successfully", overview)
        );
    }
}