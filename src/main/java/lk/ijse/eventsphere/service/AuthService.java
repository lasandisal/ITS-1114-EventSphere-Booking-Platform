package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.AuthResponseDTO;
import lk.ijse.eventsphere.dto.LoginRequestDTO;
import lk.ijse.eventsphere.dto.RegisterRequestDTO;

public interface AuthService {

    AuthResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);
}
