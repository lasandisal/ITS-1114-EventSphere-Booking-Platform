package lk.ijse.eventsphere.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Component
public class PayHereSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(PayHereSignatureUtil.class);

    @Value("${app.payhere.merchant-secret}")
    private String merchantSecret;

    public String generateCheckoutHash(String merchantId, String orderId, String formattedAmount, String currency) {
        // Step 1: UpperCase MD5 of Merchant Secret
        String secretHash = md5Hex(merchantSecret.trim()).toUpperCase();

        // Step 2: Build raw string
        String raw = merchantId.trim() + orderId.trim() + formattedAmount.trim() + currency.trim() + secretHash;

        // Step 3: Final UpperCase Hash
        String hash = md5Hex(raw).toUpperCase();

        log.info("PayHere Checkout Hash Debug:");
        log.info("  merchantId      = {}", merchantId);
        log.info("  orderId         = {}", orderId);
        log.info("  formattedAmount = {}", formattedAmount);
        log.info("  currency        = {}", currency);
        log.info("  secretHash      = {}", secretHash);
        log.info("  raw             = {}", raw);
        log.info("  generatedHash   = {}", hash);

        return hash;
    }

    public String generateNotifySignature(String merchantId, String orderId, String amount, String currency, String statusCode) {
        String secretHash = md5Hex(merchantSecret.trim()).toUpperCase();
        String raw = merchantId.trim() + orderId.trim() + amount.trim() + currency.trim() + statusCode.trim() + secretHash;
        return md5Hex(raw).toUpperCase();
    }

    private String md5Hex(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}