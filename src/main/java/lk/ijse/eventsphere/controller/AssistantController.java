package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.ChatRequestDTO;
import lk.ijse.eventsphere.dto.ChatResponseDTO;
import lk.ijse.eventsphere.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Authenticated only — get_my_bookings needs to know who's asking, and this
// being a "personal" assistant (per design) means it should never be usable
// anonymously in the first place.
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<CommonResponse<ChatResponseDTO>> chat(@Valid @RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = assistantService.chat(request);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "OK", response));
    }
}
