package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// Client-facing conversation turn — plain role/text only. Intermediate
// tool_use/tool_result blocks from the Anthropic API never reach the client;
// each new chat request re-runs tool resolution fresh rather than replaying
// stale tool results as if they were still current.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String role; // "user" or "assistant"
    private String content;
}
