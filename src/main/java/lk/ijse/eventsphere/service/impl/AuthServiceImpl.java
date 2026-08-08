package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.constant.ResponseMessage;
import lk.ijse.eventsphere.dto.AuthResponseDTO;
import lk.ijse.eventsphere.dto.LoginRequestDTO;
import lk.ijse.eventsphere.dto.RegisterRequestDTO;
import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.enums.UserStatus;
import lk.ijse.eventsphere.exception.DuplicateResourceException;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.RoleRepository;
import lk.ijse.eventsphere.repository.UserRepository;
import lk.ijse.eventsphere.security.JwtUtil;
import lk.ijse.eventsphere.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(ResponseMessage.EMAIL_ALREADY_REGISTERED);
        }

        // Every self-registration gets the USER role only. ORGANIZER is a
        // deliberate elevation (via a separate admin/organizer-application
        // endpoint, not open self-registration) so anyone can't start
        // publishing events unchecked.
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "USER role missing — ensure roles are seeded on startup"));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        // Delegates to CustomUserDetailsService + BCrypt comparison; throws
        // BadCredentialsException (handled by GlobalExceptionHandler) on
        // mismatch — never leak whether it was the email or password that
        // was wrong.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return buildAuthResponse(user);
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
