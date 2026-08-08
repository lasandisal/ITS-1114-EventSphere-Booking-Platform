package lk.ijse.eventsphere.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

// Uniform error shape returned by GlobalExceptionHandler for every failure
// path — REST clients (and the AI assistant's tool-call error handling)
// only ever need to parse one structure.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Populated only for validation failures (@Valid on request DTOs) —
    // field name -> human-readable reason.
    private Map<String, String> validationErrors;
}
