package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.UserResponseDTO;
import lk.ijse.eventsphere.entity.Organizer;
import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.OrganizerRepository;
import lk.ijse.eventsphere.repository.RoleRepository;
import lk.ijse.eventsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizerRepository organizerRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<CommonResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userRepository.findAll().stream()
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .roles(user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Users fetched successfully", users));
    }

    @PatchMapping("/{userId}/promote-to-admin")
    @Transactional
    public ResponseEntity<CommonResponse<String>> promoteToAdmin(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing"));
        Role organizerRole = roleRepository.findByName(RoleName.ORGANIZER)
                .orElseThrow(() -> new IllegalStateException("ORGANIZER role missing"));

        user.getRoles().add(adminRole);
        user.getRoles().add(organizerRole);
        userRepository.save(user);

        if (organizerRepository.findByUserId(userId).isEmpty()) {
            Organizer organizer = Organizer.builder()
                    .user(user)
                    .businessName(user.getFullName() + " (Admin)")
                    .build();
            organizerRepository.save(organizer);
        }

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "User promoted to Admin successfully", null));
    }
}