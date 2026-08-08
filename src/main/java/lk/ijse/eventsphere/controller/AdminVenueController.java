package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.VenueRequestDTO;
import lk.ijse.eventsphere.dto.VenueResponseDTO;
import lk.ijse.eventsphere.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Matches SecurityConfig's /api/v1/admin/** rule (hasRole ADMIN).
@RestController
@RequestMapping("/api/v1/admin/venues")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<CommonResponse<VenueResponseDTO>> create(
            @Valid @RequestBody VenueRequestDTO request) {
        VenueResponseDTO venue = venueService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), "Venue created", venue));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<VenueResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody VenueRequestDTO request) {
        VenueResponseDTO venue = venueService.update(id, request);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Venue updated", venue));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long id) {
        venueService.delete(id);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Venue deleted", null));
    }
}
