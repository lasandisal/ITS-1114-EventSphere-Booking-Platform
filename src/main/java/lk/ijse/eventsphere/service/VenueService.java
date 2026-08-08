package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.VenueRequestDTO;
import lk.ijse.eventsphere.dto.VenueResponseDTO;

import java.util.List;

public interface VenueService {
    VenueResponseDTO create(VenueRequestDTO request);
    VenueResponseDTO update(Long id, VenueRequestDTO request);
    void delete(Long id);
    VenueResponseDTO getById(Long id);
    List<VenueResponseDTO> getAll();
}
