package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.VenueRequestDTO;
import lk.ijse.eventsphere.dto.VenueResponseDTO;
import lk.ijse.eventsphere.entity.Venue;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.EventRepository;
import lk.ijse.eventsphere.repository.VenueRepository;
import lk.ijse.eventsphere.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public VenueResponseDTO create(VenueRequestDTO request) {
        Venue venue = Venue.builder()
                .name(request.getName())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .capacity(request.getCapacity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        venueRepository.save(venue);
        return toDto(venue);
    }

    @Override
    @Transactional
    public VenueResponseDTO update(Long id, VenueRequestDTO request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));

        venue.setName(request.getName());
        venue.setAddressLine(request.getAddressLine());
        venue.setCity(request.getCity());
        venue.setCapacity(request.getCapacity());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venueRepository.save(venue);
        return toDto(venue);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));

        if (eventRepository.existsByVenueId(id)) {
            throw new IllegalStateException(
                    "Cannot delete venue '" + venue.getName() + "' — events still reference it");
        }
        venueRepository.delete(venue);
    }

    @Override
    public VenueResponseDTO getById(Long id) {
        return venueRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
    }

    @Override
    public List<VenueResponseDTO> getAll() {
        return venueRepository.findAll().stream().map(this::toDto).toList();
    }

    private VenueResponseDTO toDto(Venue venue) {
        return VenueResponseDTO.builder()
                .id(venue.getId())
                .name(venue.getName())
                .addressLine(venue.getAddressLine())
                .city(venue.getCity())
                .capacity(venue.getCapacity())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .build();
    }
}
