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

// Open to all users — guests can search events and ask questions, while
// authenticated users can also access personalized features like booking history.
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<CommonResponse<ChatResponseDTO>> chat(@Valid @RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = assistantService.chat(request);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "OK", response));
    }
}
