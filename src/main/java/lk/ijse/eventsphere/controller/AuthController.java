package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.constant.ResponseMessage;
import lk.ijse.eventsphere.dto.AuthResponseDTO;
import lk.ijse.eventsphere.dto.LoginRequestDTO;
import lk.ijse.eventsphere.dto.RegisterRequestDTO;
import lk.ijse.eventsphere.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), ResponseMessage.REGISTRATION_SUCCESS, response));
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), ResponseMessage.LOGIN_SUCCESS, response));
    }
}
