package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.OrganizerApplicationRequestDTO;
import lk.ijse.eventsphere.dto.OrganizerResponseDTO;

public interface OrganizerService {

    OrganizerResponseDTO applyAsOrganizer(OrganizerApplicationRequestDTO request);

    OrganizerResponseDTO getMyOrganizerProfile();
}
