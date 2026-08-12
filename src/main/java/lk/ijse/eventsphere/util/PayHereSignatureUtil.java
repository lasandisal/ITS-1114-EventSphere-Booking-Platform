package lk.ijse.eventsphere.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class PayHereSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(PayHereSignatureUtil.class);

    @Value("${app.payhere.merchant-secret}")
    private String merchantSecret;

    public String generateCheckoutHash(String merchantId, String orderId, String formattedAmount, String currency) {
        String cleanSecret = getRawSecret(merchantSecret);

        // 1. Convert secret to MD5 Uppercase
        String secretHash = md5Hex(cleanSecret).toUpperCase();

        // 2. Concatenate parameters
        String raw = merchantId + orderId + formattedAmount + currency + secretHash;

        // 3. Final Checkout Hash
        String hash = md5Hex(raw).toUpperCase();

        log.info("PayHere hash debug — merchantId={}, orderId={}, amount={}, currency={}, secretHash={}, raw={}, hash={}",
                merchantId, orderId, formattedAmount, currency, secretHash, raw, hash);

        return hash;
    }

    public String generateNotifySignature(String merchantId, String orderId, String amount, String currency, String statusCode) {
        String cleanSecret = getRawSecret(merchantSecret);
        String secretHash = md5Hex(cleanSecret).toUpperCase();
        String raw = merchantId + orderId + amount + currency + statusCode + secretHash;
        return md5Hex(raw).toUpperCase();
    }

    private String getRawSecret(String secret) {
        if (secret == null) return "";
        String trimmed = secret.trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            String decodedStr = new String(decoded, StandardCharsets.UTF_8);
            return decodedStr.isEmpty() ? trimmed : decodedStr;
        } catch (Exception e) {
            return trimmed;
        }
    }

    // --- Helper Method ---
    private String md5Hex(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}