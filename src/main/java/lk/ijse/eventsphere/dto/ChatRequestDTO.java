package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    @NotBlank(message = "Message is required")
    @Size(max = 2000)
    private String message;

    // The client resends the running conversation each turn (server is
    // stateless between requests) — same pattern as any Claude API
    // integration with no server-side session store.
    private List<ChatMessageDTO> history;
}
