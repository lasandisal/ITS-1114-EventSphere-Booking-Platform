package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.OrganizerApplicationRequestDTO;
import lk.ijse.eventsphere.dto.OrganizerResponseDTO;

import java.util.List;

public interface OrganizerService {

    OrganizerResponseDTO applyAsOrganizer(OrganizerApplicationRequestDTO request);

    OrganizerResponseDTO getMyOrganizerProfile();

    List<OrganizerResponseDTO> getPendingApplications();

    OrganizerResponseDTO verifyOrganizer(Long organizerId);

    void rejectOrganizer(Long organizerId);

    List<OrganizerResponseDTO> getAllOrganizers();

    OrganizerResponseDTO updateOrganizerProfile(OrganizerApplicationRequestDTO request);
}
