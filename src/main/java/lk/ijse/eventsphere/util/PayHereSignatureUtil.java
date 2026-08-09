package lk.ijse.eventsphere.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// PayHere's own hashing scheme (MD5-based) — required by their IPG spec,
// not a choice made here. Used for both the Phase 1 checkout hash the
// frontend needs and the Phase 2 webhook signature re-verification.
@Component
public class PayHereSignatureUtil {

    @Value("${app.payhere.merchant-secret}")
    private String merchantSecret;

    // hash = MD5( merchant_id + order_id + amount + currency + MD5(secret) )
    // — sent to PayHere so it can validate the checkout request came from us.
    public String generateCheckoutHash(String merchantId, String orderId, String formattedAmount, String currency) {
        String secretHash = md5(merchantSecret).toUpperCase();
        String raw = merchantId + orderId + formattedAmount + currency + secretHash;
        return md5(raw).toUpperCase();
    }

    // Same formula plus status_code — recomputed server-side on every webhook
    // delivery and compared to the md5sig PayHere sent, so a forged callback
    // (anyone who knows our order id/amount but not merchantSecret) fails
    // this check before anything else runs.
    public String generateNotifySignature(String merchantId, String orderId, String payhereAmount,
                                          String payhereCurrency, String statusCode) {
        String secretHash = md5(merchantSecret).toUpperCase();
        String raw = merchantId + orderId + payhereAmount + payhereCurrency + statusCode + secretHash;
        return md5(raw).toUpperCase();
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }
}
