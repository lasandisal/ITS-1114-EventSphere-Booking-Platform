package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.CheckInResponseDTO;
import lk.ijse.eventsphere.dto.ScanRequestDTO;
import lk.ijse.eventsphere.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Matches SecurityConfig's /api/v1/organizer/** rule (hasAnyRole ORGANIZER,
// ADMIN) — gate staff need at least the ORGANIZER role; ownership of the
// specific event is re-checked inside CheckInServiceImpl.
@RestController
@RequestMapping("/api/v1/organizer/check-in")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/scan")
    public ResponseEntity<CommonResponse<CheckInResponseDTO>> scan(
            @Valid @RequestBody ScanRequestDTO request) {
        CheckInResponseDTO result = checkInService.scanTicket(request);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Entry granted", result));
    }
}
