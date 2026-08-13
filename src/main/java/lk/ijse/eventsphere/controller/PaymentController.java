package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.PaymentInitiationResponseDTO;
import lk.ijse.eventsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lk.ijse.eventsphere.config.PayHereProperties;
import lk.ijse.eventsphere.util.PayHereSignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PayHereProperties payHereProperties;
    private final PayHereSignatureUtil payHereSignatureUtil;

    @GetMapping("/test-checkout")
    public ResponseEntity<CommonResponse<PaymentInitiationResponseDTO>> testCheckout() {
        String merchantId = payHereProperties.getMerchantId();
        String orderId = "TEST-" + System.currentTimeMillis();
        String amount = "100.00";
        String currency = "LKR";
        String hash = payHereSignatureUtil.generateCheckoutHash(merchantId, orderId, amount, currency);

        PaymentInitiationResponseDTO dto = PaymentInitiationResponseDTO.builder()
                .merchantId(merchantId)
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .hash(hash)
                .itemsDescription("EventSphere PayHere Sandbox Test")
                .returnUrl(payHereProperties.getReturnUrl())
                .cancelUrl(payHereProperties.getCancelUrl())
                .notifyUrl(payHereProperties.getNotifyUrl())
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phone("0771234567")
                .address("No. 1, Test Street")
                .city("Colombo")
                .country("Sri Lanka")
                .actionUrl(payHereProperties.getGatewayUrl())
                .build();

        log.info("[PAYHERE ISOLATED TEST] Test Checkout DTO generated: orderId={}, merchantId={}, amount={}, hashLength={}",
                orderId, merchantId, amount, hash != null ? hash.length() : 0);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Isolated PayHere Test Checkout Ready", dto));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/initiate/{bookingId}")
    public ResponseEntity<CommonResponse<PaymentInitiationResponseDTO>> initiate(@PathVariable Long bookingId) {
        log.info("[PAYMENT DEBUG] [1. PAYMENT REQUEST RECEIVED] timestamp={}, method=POST, endpoint=/api/v1/payments/initiate/{}", 
                java.time.LocalDateTime.now(), bookingId);
        try {
            PaymentInitiationResponseDTO response = paymentService.initiatePayment(bookingId);
            log.info("[PAYMENT DEBUG] [6. RESPONSE SENT TO FRONTEND] status=200 OK, responseClass=PaymentInitiationResponseDTO, bookingId={}, orderId={}, merchantId={}, amount={}, currency={}, hashLength={}, returnUrl={}, cancelUrl={}, notifyUrl={}",
                    bookingId, response.getOrderId(), response.getMerchantId(), response.getAmount(), response.getCurrency(),
                    response.getHash() != null ? response.getHash().length() : 0,
                    response.getReturnUrl(), response.getCancelUrl(), response.getNotifyUrl());
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Checkout ready", response));
        } catch (Exception ex) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH - PAYMENT INITIATION FAILED] bookingId={}, exceptionType={}, message={}",
                    bookingId, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    @PostMapping(value = "/notify", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> notify(@RequestParam Map<String, String> params) {
        log.info("[PAYMENT DEBUG] [7. PAYHERE NOTIFY/WEBHOOK RECEIVED] timestamp={}, endpoint=/api/v1/payments/notify, merchantId={}, orderId={}, amount={}, currency={}, statusCode={}, md5sig={}",
                java.time.LocalDateTime.now(), params.get("merchant_id"), params.get("order_id"), params.get("payhere_amount"),
                params.get("payhere_currency"), params.get("status_code"), params.get("md5sig"));
        paymentService.handleNotify(params);
        return ResponseEntity.ok("OK");
    }
}
