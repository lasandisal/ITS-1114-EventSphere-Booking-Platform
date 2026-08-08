package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.VenueResponseDTO;
import lk.ijse.eventsphere.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Public — matches SecurityConfig's permitAll() for GET /api/v1/venues/**.
@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<VenueResponseDTO>>> getAll() {
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Venues retrieved", venueService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VenueResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Venue retrieved", venueService.getById(id)));
    }
}
