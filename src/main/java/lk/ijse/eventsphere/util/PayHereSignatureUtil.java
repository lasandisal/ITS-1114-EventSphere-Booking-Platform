package lk.ijse.eventsphere.util;

import lk.ijse.eventsphere.config.PayHereProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PayHereSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(PayHereSignatureUtil.class);

    private final PayHereProperties payHereProperties;

    public String generateCheckoutHash(String merchantId, String orderId, String formattedAmount, String currency) {
        String merchantSecret = payHereProperties.getMerchantSecret();
        boolean secretPresent = merchantSecret != null && !merchantSecret.isBlank();
        int secretLength = secretPresent ? merchantSecret.length() : 0;
        String secretFingerprint = safeFingerprint(merchantSecret);

        log.info("[PAYMENT DEBUG] [5. PAYHERE CONFIGURATION RESOLUTION] merchantSecret present={}, length={}, fingerprint={}",
                secretPresent, secretLength, secretFingerprint);

        // 1. Calculate MD5 of merchantSecret in LOWERCASE (Standard MD5 output)
        String secretHash = md5Hex(merchantSecret != null ? merchantSecret.trim() : "").toLowerCase();

        // 2. Concatenate: merchant_id + order_id + amount + currency + secretHash (in uppercase)
        // PayHere formula: md5(merchant_id + order_id + amount + currency + strtoupper(md5(merchant_secret)))
        String raw = (merchantId != null ? merchantId : "") 
                + (orderId != null ? orderId : "") 
                + (formattedAmount != null ? formattedAmount : "") 
                + (currency != null ? currency : "") 
                + secretHash.toUpperCase();

        // 3. Final Checkout Hash in UPPERCASE
        String hash = md5Hex(raw).toUpperCase();

        log.info("[PAYMENT DEBUG] [4. PAYHERE REQUEST CONSTRUCTION - HASH GENERATION]");
        log.info("[PAYMENT DEBUG]   Algorithm: MD5");
        log.info("[PAYMENT DEBUG]   Component merchantId: {}", merchantId);
        log.info("[PAYMENT DEBUG]   Component orderId: {}", orderId);
        log.info("[PAYMENT DEBUG]   Component formattedAmount: {}", formattedAmount);
        log.info("[PAYMENT DEBUG]   Component currency: {}", currency);
        log.info("[PAYMENT DEBUG]   Component secretHash (strtoupper(md5(secret))): {}", secretHash.toUpperCase());
        log.info("[PAYMENT DEBUG]   Raw Concatenated Input: {}", raw);
        log.info("[PAYMENT DEBUG]   Resulting Hash: present={}, length={}, fingerprint={}",
                hash != null && !hash.isBlank(), hash != null ? hash.length() : 0, safeFingerprint(hash));

        return hash;
    }

    public String generateNotifySignature(String merchantId, String orderId, String amount, String currency, String statusCode) {
        String merchantSecret = payHereProperties.getMerchantSecret();
        String secretHash = md5Hex(merchantSecret != null ? merchantSecret.trim() : "").toUpperCase();
        String raw = (merchantId != null ? merchantId : "") 
                + (orderId != null ? orderId : "") 
                + (amount != null ? amount : "") 
                + (currency != null ? currency : "") 
                + (statusCode != null ? statusCode : "") 
                + secretHash;
        String signature = md5Hex(raw).toUpperCase();

        log.info("[PAYMENT DEBUG] [7. PAYHERE NOTIFY - SIGNATURE CALCULATION]");
        log.info("[PAYMENT DEBUG]   Raw Concatenated Notify Input: {}", raw);
        log.info("[PAYMENT DEBUG]   Calculated Notify Signature: fingerprint={}", safeFingerprint(signature));

        return signature;
    }

    private String md5Hex(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String safeFingerprint(String value) {
        if (value == null || value.isBlank()) return "NULL/EMPTY";
        if (value.length() <= 8) return "***(len=" + value.length() + ")";
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4) + "(len=" + value.length() + ")";
    }
}