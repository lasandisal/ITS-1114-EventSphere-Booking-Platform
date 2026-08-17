package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.OrganizerResponseDTO;
import lk.ijse.eventsphere.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/organizers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrganizerController {

    private final OrganizerService organizerService;

    // Handles GET /api/v1/admin/organizers and GET /api/v1/admin/organizers/pending
    @GetMapping({"", "/pending"})
    public ResponseEntity<CommonResponse<List<OrganizerResponseDTO>>> getPending() {
        List<OrganizerResponseDTO> pending = organizerService.getPendingApplications();
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Pending applications retrieved", pending));
    }

    // Handles GET /api/v1/admin/organizers/all
    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<OrganizerResponseDTO>>> getAll() {
        List<OrganizerResponseDTO> all = organizerService.getAllOrganizers();
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "All organizers retrieved", all));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<CommonResponse<OrganizerResponseDTO>> verify(@PathVariable Long id) {
        OrganizerResponseDTO organizer = organizerService.verifyOrganizer(id);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(),
                "Organizer approved — ORGANIZER role granted", organizer));
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<CommonResponse<Void>> reject(@PathVariable Long id) {
        organizerService.rejectOrganizer(id);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Application rejected", null));
    }
}