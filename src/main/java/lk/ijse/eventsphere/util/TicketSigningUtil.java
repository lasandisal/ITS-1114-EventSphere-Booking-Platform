package lk.ijse.eventsphere.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

// Signs the QR payload (ticket_code + HMAC-SHA256) so a gate scanner can
// reject a forged/guessed code before ever hitting the database — the
// design decided on when the schema was drawn up. The secret is app-level
// config, never derived from or stored alongside the ticket_code itself.
@Component
public class TicketSigningUtil {

    @Value("${app.ticket.qr-secret}")
    private String secret;

    // What actually gets encoded into the QR image.
    public String buildSignedPayload(String ticketCode) {
        String signature = hmacSha256(ticketCode);
        String raw = ticketCode + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // Used by the (upcoming) check-in scan flow: decode the QR text, split
    // on ":", recompute the HMAC for the ticket_code half, and compare —
    // constant-time, to avoid a timing side-channel on signature comparison.
    public boolean verifySignedPayload(String signedPayload) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(signedPayload), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) return false;
            String ticketCode = parts[0];
            String providedSignature = parts[1];
            String expectedSignature = hmacSha256(ticketCode);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false; // any malformed payload is simply invalid, not an error
        }
    }

    public String extractTicketCode(String signedPayload) {
        String decoded = new String(Base64.getUrlDecoder().decode(signedPayload), StandardCharsets.UTF_8);
        return decoded.split(":", 2)[0];
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute ticket signature", e);
        }
    }
}
