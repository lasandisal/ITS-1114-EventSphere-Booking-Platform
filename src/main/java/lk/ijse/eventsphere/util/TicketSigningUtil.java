package lk.ijse.eventsphere.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class TicketSigningUtil {

    @Value("${app.ticket.qr-secret:SuperSecretKeyForSigningEventSphereTickets2026!}")
    private String secret;

    public String buildSignedPayload(String ticketCode) {
        String signature = hmacSha256(ticketCode);
        String raw = ticketCode + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

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
            return false;
        }
    }

    public String extractTicketCode(String signedPayload) {
        String decoded = new String(Base64.getUrlDecoder().decode(signedPayload), StandardCharsets.UTF_8);
        return decoded.split(":", 2)[0];
    }

    private String hmacSha256(String data) {
        try {
            // Guard clause: if property resolves to empty string, use hardcoded backup
            String activeSecret = (secret != null && !secret.isBlank())
                    ? secret
                    : "SuperSecretKeyForSigningEventSphereTickets2026!";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(activeSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute ticket signature: " + e.getMessage(), e);
        }
    }
}