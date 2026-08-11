package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.ChatRequestDTO;
import lk.ijse.eventsphere.dto.ChatResponseDTO;

public interface AssistantService {

    ChatResponseDTO chat(ChatRequestDTO request);
}
