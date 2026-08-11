package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScanRequestDTO {

    // The full base64url payload decoded straight off the QR image
    // (ticketCode + ":" + HMAC signature) — not a bare ticket code.
    @NotBlank(message = "Scanned QR payload is required")
    private String signedPayload;

    private String locationNote;
}
