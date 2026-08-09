package lk.ijse.eventsphere.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.eventsphere.dto.PaymentInitiationResponseDTO;
import lk.ijse.eventsphere.entity.*;
import lk.ijse.eventsphere.enums.BookingStatus;
import lk.ijse.eventsphere.enums.PaymentStatus;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.BookingRepository;
import lk.ijse.eventsphere.repository.PaymentLogRepository;
import lk.ijse.eventsphere.repository.PaymentRepository;
import lk.ijse.eventsphere.security.CurrentUserProvider;
import lk.ijse.eventsphere.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUserProvider;
    private final BookingService bookingService;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;
    private final lk.ijse.eventsphere.util.PayHereSignatureUtil signatureUtil;
    private final lk.ijse.eventsphere.util.TicketSigningUtil ticketSigningUtil;
    private final ObjectMapper objectMapper;

    @Value("${app.payhere.merchant-id}")
    private String merchantId;

    @Value("${app.payhere.currency:LKR}")
    private String currency;

    @Value("${app.payhere.return-url}")
    private String returnUrl;

    @Value("${app.payhere.cancel-url}")
    private String cancelUrl;

    @Value("${app.payhere.notify-url}")
    private String notifyUrl;

    @Override
    @Transactional
    public PaymentInitiationResponseDTO initiatePayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        User caller = currentUserProvider.getCurrentUser();
        if (!booking.getUser().getId().equals(caller.getId())) {
            throw new AccessDeniedException("You do not own this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("This booking is not awaiting payment (status: " + booking.getStatus() + ")");
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This booking's hold has expired — please book again");
        }

        String formattedAmount = String.format("%.2f", booking.getTotalAmount());

        // Reuse the existing Payment row on a retry (e.g. user reloads the
        // checkout page) instead of violating the payments.booking_id unique
        // constraint with a second insert.
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("This booking has already been paid for");
        }
        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .provider("PAYHERE")
                    .merchantOrderId(booking.getBookingReference())
                    .amount(booking.getTotalAmount())
                    .currency(currency)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);
        }

        String hash = signatureUtil.generateCheckoutHash(merchantId, payment.getMerchantOrderId(), formattedAmount, currency);

        String[] nameParts = splitName(caller.getFullName());

        return PaymentInitiationResponseDTO.builder()
                .merchantId(merchantId)
                .orderId(payment.getMerchantOrderId())
                .amount(formattedAmount)
                .currency(currency)
                .hash(hash)
                .itemsDescription("EventSphere booking — " + booking.getEvent().getTitle())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .notifyUrl(notifyUrl)
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .email(caller.getEmail())
                .phone(caller.getPhone() != null ? caller.getPhone() : "0000000000")
                .address("N/A")
                .city("N/A")
                .country("Sri Lanka")
                .build();
    }

    @Override
    @Transactional
    public void handleNotify(Map<String, String> params) {
        // Log the raw callback FIRST and unconditionally — this is the audit
        // trail regardless of what happens next, including malformed or
        // fraudulent callbacks.
        PaymentLog paymentLog = PaymentLog.builder()
                .rawPayload(toJson(params))
                .statusCode(params.get("status_code"))
                .processed(false)
                .build();

        String merchantIdReceived = params.get("merchant_id");
        String orderId = params.get("order_id");
        String payhereAmount = params.get("payhere_amount");
        String payhereCurrency = params.get("payhere_currency");
        String statusCode = params.get("status_code");
        String receivedSig = params.get("md5sig");

        Payment payment = paymentRepository.findByMerchantOrderId(orderId).orElse(null);
        paymentLog.setPayment(payment);
        paymentLogRepository.save(paymentLog);

        if (payment == null) {
            log.warn("PayHere webhook for unknown order_id={}, logged and ignored", orderId);
            return;
        }

        String expectedSig = signatureUtil.generateNotifySignature(
                merchantIdReceived, orderId, payhereAmount, payhereCurrency, statusCode);

        if (!expectedSig.equalsIgnoreCase(receivedSig)) {
            // Do NOT throw here — a bad signature will never become valid on
            // retry, so there's nothing to gain from PayHere resending it.
            // Logged above with processed=false; investigate via payment_logs.
            log.warn("PayHere webhook signature mismatch for order_id={} — ignoring callback", orderId);
            return;
        }

        // Idempotency: a webhook can be delivered more than once. If we've
        // already processed this to SUCCESS, do nothing further (no double
        // ticket issuance, no duplicate email).
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            paymentLog.setProcessed(true);
            paymentLogRepository.save(paymentLog);
            return;
        }

        switch (statusCode) {
            case "2" -> confirmPayment(payment, receivedSig);
            case "-1", "-2", "-3" -> failPayment(payment);
            default -> log.info("PayHere status_code={} for order_id={} — no action, awaiting resolution",
                    statusCode, orderId);
        }

        paymentLog.setProcessed(true);
        paymentLogRepository.save(paymentLog);
    }

    // ==================== helpers ====================

    private void confirmPayment(Payment payment, String receivedSig) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setMd5Signature(receivedSig);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        List<TicketEmailItem> emailItems = new ArrayList<>();
        for (BookingItem item : booking.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                String signedPayload = ticketSigningUtil.buildSignedPayload(ticket.getTicketCode());
                byte[] qrPng = qrCodeService.generateQrPng(signedPayload, 300);
                ticket.setIssuedAt(LocalDateTime.now());

                emailItems.add(TicketEmailItem.builder()
                        .attendeeName(ticket.getAttendeeName())
                        .seatNumber(ticket.getSeatNumber())
                        .ticketCode(ticket.getTicketCode())
                        .qrPng(qrPng)
                        .build());
            }
        }
        // Tickets are dirty-checked and flushed with the transaction commit
        // (issued_at set above) — no explicit save needed for managed entities.

        emailService.sendBookingConfirmation(
                booking.getUser().getEmail(),
                booking.getUser().getFullName(),
                booking.getEvent().getTitle(),
                booking.getBookingReference(),
                emailItems);

        log.info("Payment confirmed and {} ticket(s) issued for booking {}",
                emailItems.size(), booking.getBookingReference());
    }

    private void failPayment(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        bookingService.releaseFailedPaymentBooking(payment.getBooking().getId());
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"Guest", ""};
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[]{parts[0], ""};
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{\"error\":\"failed to serialize webhook payload\"}";
        }
    }
}
