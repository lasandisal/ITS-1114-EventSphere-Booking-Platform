package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.OrganizerApplicationRequestDTO;
import lk.ijse.eventsphere.dto.OrganizerResponseDTO;
import lk.ijse.eventsphere.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    // Any authenticated USER can apply — becomes ORGANIZER on success.
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/apply")
    public ResponseEntity<CommonResponse<OrganizerResponseDTO>> apply(
            @Valid @RequestBody OrganizerApplicationRequestDTO request) {
        OrganizerResponseDTO response = organizerService.applyAsOrganizer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(),
                        "Application submitted — pending admin review", response));
    }

    // Handles both GET /api/v1/organizer/me and GET /api/v1/organizer/profile
    @PreAuthorize("isAuthenticated()")
    @GetMapping({"/me", "/profile"})
    public ResponseEntity<CommonResponse<OrganizerResponseDTO>> getMyProfile() {
        OrganizerResponseDTO response = organizerService.getMyOrganizerProfile();
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Organizer profile retrieved", response));
    }

    // Handles both PUT /api/v1/organizer/me and PUT /api/v1/organizer/profile
    @PreAuthorize("hasRole('ORGANIZER')")
    @PutMapping({"/me", "/profile"})
    public ResponseEntity<CommonResponse<OrganizerResponseDTO>> updateProfile(
            @Valid @RequestBody OrganizerApplicationRequestDTO request) {
        OrganizerResponseDTO response = organizerService.updateOrganizerProfile(request);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Organizer profile updated successfully", response));
    }
}