package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.OrganizerApplicationRequestDTO;
import lk.ijse.eventsphere.dto.OrganizerResponseDTO;
import lk.ijse.eventsphere.entity.Organizer;
import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.OrganizerStatus;
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

        if (request.getApplicantPhone() != null && !request.getApplicantPhone().isBlank()
                && (user.getPhone() == null || user.getPhone().isBlank())) {
            user.setPhone(request.getApplicantPhone().trim());
            userRepository.save(user);
        }

        Organizer organizer = Organizer.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .bio(request.getBio())
                .nicOrPassportNumber(request.getNicOrPassportNumber())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .status(OrganizerStatus.PENDING)
                .isVerified(false)
                .build();
        organizerRepository.save(organizer);

        return toDto(organizer);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerResponseDTO getMyOrganizerProfile() {
        User user = currentUserProvider.getCurrentUser();
        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No organizer application for this account — apply first"));
        return toDto(organizer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerResponseDTO> getPendingApplications() {
        return organizerRepository.findByStatus(OrganizerStatus.PENDING).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerResponseDTO> getAllOrganizers() {
        return organizerRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public OrganizerResponseDTO updateOrganizerProfile(OrganizerApplicationRequestDTO request) {
        User user = currentUserProvider.getCurrentUser();
        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer profile not found for this account"));

        organizer.setBusinessName(request.getBusinessName());
        organizer.setBio(request.getBio());
        organizer.setNicOrPassportNumber(request.getNicOrPassportNumber());
        organizer.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());

        organizerRepository.save(organizer);
        return toDto(organizer);
    }

    @Override
    @Transactional
    public OrganizerResponseDTO verifyOrganizer(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer application not found: " + organizerId));

        if (organizer.getStatus() == OrganizerStatus.APPROVED && organizer.isVerified()) {
            return toDto(organizer);
        }

        organizer.setStatus(OrganizerStatus.APPROVED);
        organizer.setVerified(true);
        organizerRepository.save(organizer);

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

        if (organizer.getStatus() == OrganizerStatus.APPROVED) {
            throw new IllegalStateException("Cannot reject an already-approved organizer — revoke access instead");
        }

        organizerRepository.delete(organizer);
    }

    private OrganizerResponseDTO toDto(Organizer organizer) {
        User user = organizer.getUser();
        return OrganizerResponseDTO.builder()
                .id(organizer.getId())
                .businessName(organizer.getBusinessName())
                .applicantName(user != null ? user.getFullName() : null)
                .applicantEmail(user != null ? user.getEmail() : null)
                .applicantPhone(user != null ? user.getPhone() : null)
                .nicOrPassportNumber(organizer.getNicOrPassportNumber())
                .businessRegistrationNumber(organizer.getBusinessRegistrationNumber())
                .bio(organizer.getBio())
                .status(organizer.getStatus() != null ? organizer.getStatus().name() : OrganizerStatus.PENDING.name())
                .verified(organizer.isVerified())
                .createdAt(organizer.getCreatedAt())
                .build();
    }
}