package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.OrganizerApplicationRequestDTO;
import lk.ijse.eventsphere.dto.OrganizerResponseDTO;
import lk.ijse.eventsphere.entity.Organizer;
import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.exception.DuplicateResourceException;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.OrganizerRepository;
import lk.ijse.eventsphere.repository.RoleRepository;
import lk.ijse.eventsphere.repository.UserRepository;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizerServiceImpl implements OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public OrganizerResponseDTO applyAsOrganizer(OrganizerApplicationRequestDTO request) {
        User user = currentUserProvider.getCurrentUser();

        if (organizerRepository.findByUserId(user.getId()).isPresent()) {
            throw new DuplicateResourceException("An organizer profile already exists for this account");
        }

        Organizer organizer = Organizer.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .bio(request.getBio())
                .verified(false) // admin can verify later; not a login gate
                .build();
        organizerRepository.save(organizer);

        // Grant ORGANIZER in addition to their existing roles (typically USER) —
        // a user is never demoted from USER when they become an organizer, since
        // they can still browse/book as an attendee too.
        Role organizerRole = roleRepository.findByName(RoleName.ORGANIZER)
                .orElseThrow(() -> new IllegalStateException(
                        "ORGANIZER role missing — ensure roles are seeded on startup"));
        user.getRoles().add(organizerRole);
        userRepository.save(user);

        return toDto(organizer);
    }

    @Override
    public OrganizerResponseDTO getMyOrganizerProfile() {
        User user = currentUserProvider.getCurrentUser();
        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No organizer profile for this account"));
        return toDto(organizer);
    }

    private OrganizerResponseDTO toDto(Organizer organizer) {
        return OrganizerResponseDTO.builder()
                .id(organizer.getId())
                .userId(organizer.getUser().getId())
                .businessName(organizer.getBusinessName())
                .bio(organizer.getBio())
                .verified(organizer.isVerified())
                .build();
    }
}
