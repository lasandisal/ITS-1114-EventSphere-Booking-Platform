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

import java.util.List;

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
            throw new DuplicateResourceException(
                    "An organizer application already exists for this account (pending or approved)");
        }

        Organizer organizer = Organizer.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .bio(request.getBio())
                .nicOrPassportNumber(request.getNicOrPassportNumber())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .verified(false) // PENDING — the ORGANIZER role is NOT granted until an admin approves
                .build();
        organizerRepository.save(organizer);

        // Deliberately no role grant here — see verifyOrganizer(). Applying
        // creates a review-queue entry, not organizer access.
        return toDto(organizer);
    }

    @Override
    public OrganizerResponseDTO getMyOrganizerProfile() {
        User user = currentUserProvider.getCurrentUser();
        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No organizer application for this account — apply first"));
        return toDto(organizer);
    }

    @Override
    public List<OrganizerResponseDTO> getPendingApplications() {
        return organizerRepository.findByVerifiedFalse().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public OrganizerResponseDTO verifyOrganizer(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer application not found: " + organizerId));

        if (organizer.isVerified()) {
            return toDto(organizer); // idempotent — approving twice is a no-op, not an error
        }

        organizer.setVerified(true);
        organizerRepository.save(organizer);

        // The ORGANIZER role is granted HERE, on approval — not at
        // application time. This is the actual access gate; everything
        // downstream (@PreAuthorize on event/ticket-type endpoints) relies
        // on this role, not on the verified flag directly.
        User user = organizer.getUser();
        Role organizerRole = roleRepository.findByName(RoleName.ORGANIZER)
                .orElseThrow(() -> new IllegalStateException(
                        "ORGANIZER role missing — ensure roles are seeded on startup"));
        user.getRoles().add(organizerRole);
        userRepository.save(user);

        return toDto(organizer);
    }

    @Override
    @Transactional
    public void rejectOrganizer(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer application not found: " + organizerId));

        if (organizer.isVerified()) {
            throw new IllegalStateException("Cannot reject an already-approved organizer — revoke access instead");
        }

        // No role was ever granted for a PENDING application, so rejection
        // is just removing the application record — the user can reapply.
        organizerRepository.delete(organizer);
    }

    private OrganizerResponseDTO toDto(Organizer organizer) {
        return OrganizerResponseDTO.builder()
                .id(organizer.getId())
                .userId(organizer.getUser().getId())
                .businessName(organizer.getBusinessName())
                .bio(organizer.getBio())
                .nicOrPassportNumber(organizer.getNicOrPassportNumber())
                .businessRegistrationNumber(organizer.getBusinessRegistrationNumber())
                .verified(organizer.isVerified())
                .build();
    }
}
