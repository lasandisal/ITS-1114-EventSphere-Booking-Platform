package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.OrganizerAnalyticsOverviewDTO;
import lk.ijse.eventsphere.service.OrganizerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizer/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerAnalyticsController {

    private final OrganizerAnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<CommonResponse<OrganizerAnalyticsOverviewDTO>> getOverview() {
        OrganizerAnalyticsOverviewDTO overview = analyticsService.getOverview();
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Organizer analytics overview retrieved successfully", overview));
    }
}