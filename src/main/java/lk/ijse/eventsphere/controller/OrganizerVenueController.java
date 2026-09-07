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

@RestController
@RequestMapping("/api/v1/organizer/venues")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerVenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<CommonResponse<VenueResponseDTO>> createCustomVenue(
            @Valid @RequestBody VenueRequestDTO request) {
        VenueResponseDTO venue = venueService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), "Custom venue added successfully", venue));
    }
}
