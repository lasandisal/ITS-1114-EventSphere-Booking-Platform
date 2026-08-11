package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {
    private String reply;
    private List<ChatMessageDTO> history; // pass this back on the next request
}
