package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.*;

public interface AuthService {

    RegisterResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO verifyOtp(VerifyOtpRequestDTO request);

    void resendOtp(ResendOtpRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);

    void forgotPassword(ForgotPasswordRequestDTO request);

    void resetPassword(ResetPasswordRequestDTO request);
}
