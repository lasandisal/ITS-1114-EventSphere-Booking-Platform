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
        // 1. Hash plain merchantSecret directly to MD5 Uppercase
        String secretHash = md5Hex(merchantSecret.trim()).toUpperCase();

        // 2. Concatenate parameters: merchant_id + order_id + amount + currency + secretHash
        String raw = merchantId + orderId + formattedAmount + currency + secretHash;

        // 3. Final Checkout Hash
        String hash = md5Hex(raw).toUpperCase();

        log.info("PayHere hash debug — merchantId={}, orderId={}, amount={}, currency={}, secretHash={}, raw={}, hash={}",
                merchantId, orderId, formattedAmount, currency, secretHash, raw, hash);

        return hash;
    }

    public String generateNotifySignature(String merchantId, String orderId, String amount, String currency, String statusCode) {
        String secretHash = md5Hex(merchantSecret.trim()).toUpperCase();
        String raw = merchantId + orderId + amount + currency + statusCode + secretHash;
        return md5Hex(raw).toUpperCase();
    }

    private String md5Hex(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}