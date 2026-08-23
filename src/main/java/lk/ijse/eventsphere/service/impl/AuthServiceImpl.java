package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.constant.ResponseMessage;
import lk.ijse.eventsphere.dto.*;
import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.enums.UserStatus;
import lk.ijse.eventsphere.exception.DuplicateResourceException;
import lk.ijse.eventsphere.exception.EmailNotVerifiedException;
import lk.ijse.eventsphere.exception.InvalidOtpException;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.RoleRepository;
import lk.ijse.eventsphere.repository.UserRepository;
import lk.ijse.eventsphere.security.JwtUtil;
import lk.ijse.eventsphere.service.AuthService;
import lk.ijse.eventsphere.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.isEmailVerified()) {
                throw new DuplicateResourceException(ResponseMessage.EMAIL_ALREADY_REGISTERED);
            }

            // User registered previously but never verified their OTP — update details and refresh OTP
            String otp = generateOtp();
            existingUser.setFullName(request.getFullName());
            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existingUser.setPhone(request.getPhone());
            existingUser.setVerificationOtp(otp);
            existingUser.setVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(existingUser);

            emailService.sendVerificationOtpEmail(existingUser.getEmail(), existingUser.getFullName(), otp);

            return RegisterResponseDTO.builder()
                    .userId(existingUser.getId())
                    .email(existingUser.getEmail())
                    .fullName(existingUser.getFullName())
                    .requiresVerification(true)
                    .message("Registration initiated. A 6-digit verification code has been sent to your email.")
                    .build();
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "USER role missing — ensure roles are seeded on startup"));

        String otp = generateOtp();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .verificationOtp(otp)
                .verificationOtpExpiresAt(LocalDateTime.now().plusMinutes(10))
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        emailService.sendVerificationOtpEmail(user.getEmail(), user.getFullName(), otp);

        return RegisterResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .requiresVerification(true)
                .message("Registration successful. Please verify your email with the 6-digit OTP sent to your inbox.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO verifyOtp(VerifyOtpRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            return buildAuthResponse(user);
        }

        if (user.getVerificationOtp() == null || !user.getVerificationOtp().equals(request.getOtp().trim())) {
            throw new InvalidOtpException("Invalid verification code. Please check your OTP and try again.");
        }

        if (user.getVerificationOtpExpiresAt() != null && user.getVerificationOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("Verification code has expired. Please request a new code.");
        }

        user.setEmailVerified(true);
        user.setVerificationOtp(null);
        user.setVerificationOtpExpiresAt(null);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Your email is already verified. Please log in directly.");
        }

        String otp = generateOtp();
        user.setVerificationOtp(otp);
        user.setVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendVerificationOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!user.isEmailVerified()) {
            String otp = generateOtp();
            user.setVerificationOtp(otp);
            user.setVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            emailService.sendVerificationOtpEmail(user.getEmail(), user.getFullName(), otp);
            throw new EmailNotVerifiedException("Email is not verified. A new 6-digit verification code has been sent to your email.");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        String otp = generateOtp();
        user.setPasswordResetOtp(otp);
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        if (user.getPasswordResetOtp() == null || !user.getPasswordResetOtp().equals(request.getOtp().trim())) {
            throw new InvalidOtpException("Invalid password reset code. Please check your OTP and try again.");
        }

        if (user.getPasswordResetOtpExpiresAt() != null && user.getPasswordResetOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("Password reset code has expired. Please request a new code.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetOtp(null);
        user.setPasswordResetOtpExpiresAt(null);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user);

        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .expiresInMs(jwtExpirationMs)
                .build();
    }
}
