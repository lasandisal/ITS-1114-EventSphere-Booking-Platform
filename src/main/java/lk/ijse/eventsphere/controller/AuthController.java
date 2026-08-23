package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.constant.ResponseMessage;
import lk.ijse.eventsphere.dto.*;
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
    public ResponseEntity<CommonResponse<RegisterResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), ResponseMessage.REGISTRATION_SUCCESS, response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<AuthResponseDTO>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request) {
        AuthResponseDTO response = authService.verifyOtp(request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Email verified successfully", response));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<CommonResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequestDTO request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "A new verification code has been sent to your email", null));
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), ResponseMessage.LOGIN_SUCCESS, response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<CommonResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Password reset verification code has been sent to your email.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CommonResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Password has been reset successfully. You can now log in.", null));
    }
}
