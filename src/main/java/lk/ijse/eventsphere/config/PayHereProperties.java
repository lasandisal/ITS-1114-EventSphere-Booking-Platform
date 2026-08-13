package lk.ijse.eventsphere.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
public class PayHereProperties {

    @Value("${app.payhere.mode:sandbox}")
    private String mode;

    @Value("${app.payhere.merchant-id:}")
    private String merchantId;

    @Value("${app.payhere.merchant-secret:}")
    private String merchantSecret;

    @Value("${app.payhere.currency:LKR}")
    private String currency;

    @Value("${app.payhere.return-url:}")
    private String returnUrl;

    @Value("${app.payhere.cancel-url:}")
    private String cancelUrl;

    @Value("${app.payhere.notify-url:}")
    private String notifyUrl;

    @Value("${app.payhere.sandbox-url:https://sandbox.payhere.lk/pay/checkout}")
    private String sandboxUrl;

    @Value("${app.payhere.live-url:https://www.payhere.lk/pay/checkout}")
    private String liveUrl;

    @PostConstruct
    public void validateProperties() {
        log.info("[PAYHERE CONFIG] Validating PayHere configuration properties on startup...");

        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalStateException("FATAL: PAYHERE_MERCHANT_ID environment variable is missing or blank!");
        }

        if (merchantSecret == null || merchantSecret.isBlank()) {
            throw new IllegalStateException("FATAL: PAYHERE_MERCHANT_SECRET environment variable is missing or blank!");
        }

        if (returnUrl == null || returnUrl.isBlank()) {
            throw new IllegalStateException("FATAL: PAYHERE_RETURN_URL environment variable is missing or blank!");
        }

        if (cancelUrl == null || cancelUrl.isBlank()) {
            throw new IllegalStateException("FATAL: PAYHERE_CANCEL_URL environment variable is missing or blank!");
        }

        if (notifyUrl == null || notifyUrl.isBlank()) {
            throw new IllegalStateException("FATAL: PAYHERE_NOTIFY_URL environment variable is missing or blank!");
        }

        log.info("[PAYHERE CONFIG] PayHere configuration loaded successfully. Mode: {}, Merchant ID: {}, Gateway URL: {}",
                getMode(), getMerchantId(), getGatewayUrl());
    }

    public String getGatewayUrl() {
        if ("live".equalsIgnoreCase(mode) || "production".equalsIgnoreCase(mode)) {
            return liveUrl;
        }
        return sandboxUrl;
    }
}
