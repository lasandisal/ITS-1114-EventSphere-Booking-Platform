package lk.ijse.eventsphere.constant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResponse {
    private int status;
    private Object body;
    private String message;

    public CommonResponse(int status, String message) {
        this.message = message;
        this.status = status;
    }
}
