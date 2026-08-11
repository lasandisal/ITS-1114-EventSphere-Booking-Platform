package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.CheckInResponseDTO;
import lk.ijse.eventsphere.dto.ScanRequestDTO;

public interface CheckInService {

    CheckInResponseDTO scanTicket(ScanRequestDTO request);
}
