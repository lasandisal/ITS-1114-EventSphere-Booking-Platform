package lk.ijse.eventsphere.constant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// Uniform success-response envelope — every controller returns
// CommonResponse<T> so clients (including the AI assistant's tool results)
// always parse the same { status, message, data } shape. Failures instead go
// through ErrorResponseDTO via GlobalExceptionHandler — the two are
// deliberately distinct shapes so a client can branch on HTTP status alone.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private int status;
    private String message;
    private T data;

    public static <T> CommonResponse<T> of(int status, String message, T data) {
        return CommonResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}
