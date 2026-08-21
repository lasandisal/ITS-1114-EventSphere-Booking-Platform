package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.UserResponseDTO;
import lk.ijse.eventsphere.dto.UserUpdateRequestDTO;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.repository.UserRepository;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    // Handles GET /api/v1/users/me and GET /api/v1/users/profile
    @GetMapping({"/me", "/profile"})
    @Transactional(readOnly = true)
    public ResponseEntity<CommonResponse<UserResponseDTO>> getCurrentUserProfile() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "User profile retrieved", toDto(user)));
    }

    // Handles PUT /api/v1/users/me and PUT /api/v1/users/profile
    @PutMapping({"/me", "/profile"})
    @Transactional
    public ResponseEntity<CommonResponse<UserResponseDTO>> updateProfile(
            @Valid @RequestBody UserUpdateRequestDTO request) {
        User user = currentUserProvider.getCurrentUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "User profile updated successfully", toDto(user)));
    }

    // Handles PATCH /api/v1/users/me and PATCH /api/v1/users/profile
    @PatchMapping({"/me", "/profile"})
    @Transactional
    public ResponseEntity<CommonResponse<UserResponseDTO>> patchProfile(
            @RequestBody UserUpdateRequestDTO request) {
        User user = currentUserProvider.getCurrentUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "User profile patched successfully", toDto(user)));
    }

    private UserResponseDTO toDto(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}