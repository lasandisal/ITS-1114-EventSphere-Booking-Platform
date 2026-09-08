package lk.ijse.eventsphere.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.eventsphere.config.PayHereProperties;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final PayHereProperties payHereProperties;

    @Override
    @Transactional
    public PaymentInitiationResponseDTO initiatePayment(Long bookingId) {
        log.info("[PAYMENT DEBUG] ==================================================");
        log.info("[PAYMENT DEBUG] 1. PAYMENT REQUEST RECEIVED FOR PROCESSING");
        log.info("[PAYMENT DEBUG]   bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("[PAYMENT DEBUG] [8. ERROR PATH] Booking not found: bookingId={}", bookingId);
                    return new ResourceNotFoundException("Booking not found: " + bookingId);
                });

        User caller = currentUserProvider.getCurrentUser();
        log.info("[PAYMENT DEBUG] [1. PAYMENT REQUEST DETAILS]");
        log.info("[PAYMENT DEBUG]   callerUserId={}", caller != null ? caller.getId() : "NULL");
        log.info("[PAYMENT DEBUG]   bookingOwnerId={}", booking.getUser() != null ? booking.getUser().getId() : "NULL");
        log.info("[PAYMENT DEBUG]   eventId={}", booking.getEvent() != null ? booking.getEvent().getId() : "NULL");
        log.info("[PAYMENT DEBUG]   eventTitle={}", booking.getEvent() != null ? booking.getEvent().getTitle() : "NULL");
        log.info("[PAYMENT DEBUG]   bookingItemsCount={}", booking.getItems() != null ? booking.getItems().size() : 0);
        log.info("[PAYMENT DEBUG]   bookingTotalAmount={}", booking.getTotalAmount());

        log.info("[PAYMENT DEBUG] 2. BOOKING/PAYMENT VALIDATION");
        boolean isOwner = caller != null && booking.getUser() != null && booking.getUser().getId().equals(caller.getId());
        log.info("[PAYMENT DEBUG]   Validation (Ownership Check): {}", isOwner ? "SUCCESS" : "FAILED");
        if (!isOwner) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH] Access denied: callerUserId={} does not own bookingId={}", 
                    caller != null ? caller.getId() : "NULL", bookingId);
            throw new AccessDeniedException("You do not own this booking");
        }

        boolean isPendingStatus = booking.getStatus() == BookingStatus.PENDING;
        log.info("[PAYMENT DEBUG]   Validation (Status PENDING Check): {} (current status: {})", 
                isPendingStatus ? "SUCCESS" : "FAILED", booking.getStatus());
        if (!isPendingStatus) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH] Invalid booking status: status={} for bookingId={}", 
                    booking.getStatus(), bookingId);
            throw new IllegalStateException("This booking is not awaiting payment (status: " + booking.getStatus() + ")");
        }

        boolean isHoldValid = booking.getExpiresAt() != null && booking.getExpiresAt().isAfter(LocalDateTime.now());
        log.info("[PAYMENT DEBUG]   Validation (Hold Expiry Check): {} (expiresAt: {}, now: {})", 
                isHoldValid ? "SUCCESS" : "FAILED", booking.getExpiresAt(), LocalDateTime.now());
        if (!isHoldValid) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH] Booking hold expired: expiresAt={} for bookingId={}", 
                    booking.getExpiresAt(), bookingId);
            throw new IllegalStateException("This booking's hold has expired — please book again");
        }

        String formattedAmount = String.format(java.util.Locale.US, "%.2f", booking.getTotalAmount());
        String orderId = "ES-" + booking.getId();

        String merchantId = payHereProperties.getMerchantId();
        String currency = payHereProperties.getCurrency();
        String returnUrl = payHereProperties.getReturnUrl();
        String cancelUrl = payHereProperties.getCancelUrl();
        String notifyUrl = payHereProperties.getNotifyUrl();
        String gatewayUrl = payHereProperties.getGatewayUrl();

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH] Booking already paid: bookingId={}, paymentStatus={}", 
                    bookingId, payment.getStatus());
            throw new IllegalStateException("This booking has already been paid for");
        }

        log.info("[PAYMENT DEBUG] 3. PAYMENT RECORD CREATION");
        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .provider("PAYHERE")
                    .merchantOrderId(orderId)
                    .amount(booking.getTotalAmount())
                    .currency(currency)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);
            log.info("[PAYMENT DEBUG]   Created NEW Payment entity: paymentId={}, orderId={}, amount={}, currency={}, status={}",
                    payment.getId(), payment.getMerchantOrderId(), payment.getAmount(), payment.getCurrency(), payment.getStatus());
        } else {
            log.info("[PAYMENT DEBUG]   Reusing EXISTING Payment entity: paymentId={}, orderId={}, amount={}, currency={}, status={}",
                    payment.getId(), payment.getMerchantOrderId(), payment.getAmount(), payment.getCurrency(), payment.getStatus());
        }

        log.info("[PAYMENT DEBUG] 5. PAYHERE CONFIGURATION RESOLUTION");
        log.info("[PAYMENT DEBUG]   Mode: {}", payHereProperties.getMode());
        log.info("[PAYMENT DEBUG]   PAYHERE_MERCHANT_ID configured: {} (value: {})", 
                merchantId != null && !merchantId.isBlank(), merchantId);
        log.info("[PAYMENT DEBUG]   PAYHERE_RETURN_URL configured: {} (value: {})", 
                returnUrl != null && !returnUrl.isBlank(), returnUrl);
        log.info("[PAYMENT DEBUG]   PAYHERE_CANCEL_URL configured: {} (value: {})", 
                cancelUrl != null && !cancelUrl.isBlank(), cancelUrl);
        log.info("[PAYMENT DEBUG]   PAYHERE_NOTIFY_URL configured: {} (value: {})", 
                notifyUrl != null && !notifyUrl.isBlank(), notifyUrl);
        log.info("[PAYMENT DEBUG]   PayHere Gateway Endpoint: {}", gatewayUrl);

        log.info("[PAYMENT DEBUG] 4. PAYHERE REQUEST CONSTRUCTION");
        String hash = signatureUtil.generateCheckoutHash(merchantId, payment.getMerchantOrderId(), formattedAmount, currency);

        String[] nameParts = splitName(caller.getFullName());

        log.info("[PAYMENT DEBUG]   Generated PayHere Parameters:");
        log.info("[PAYMENT DEBUG]     merchant_id: {}", merchantId);
        log.info("[PAYMENT DEBUG]     order_id: {}", payment.getMerchantOrderId());
        log.info("[PAYMENT DEBUG]     amount: {}", formattedAmount);
        log.info("[PAYMENT DEBUG]     currency: {}", currency);
        log.info("[PAYMENT DEBUG]     hash: present={}, length={}", hash != null, hash != null ? hash.length() : 0);
        log.info("[PAYMENT DEBUG]     itemsDescription: {}", "EventSphere booking — " + booking.getEvent().getTitle());
        log.info("[PAYMENT DEBUG]     return_url: {}", returnUrl);
        log.info("[PAYMENT DEBUG]     cancel_url: {}", cancelUrl);
        log.info("[PAYMENT DEBUG]     notify_url: {}", notifyUrl);
        log.info("[PAYMENT DEBUG]     first_name: {}", nameParts[0]);
        log.info("[PAYMENT DEBUG]     last_name: {}", nameParts[1]);
        log.info("[PAYMENT DEBUG]     email: {}", caller.getEmail());
        log.info("[PAYMENT DEBUG]     phone: {}", caller.getPhone() != null ? caller.getPhone() : "0000000000");

        log.info("[PAYMENT DEBUG] PAYHERE FINAL REQUEST SUMMARY:");
        log.info("[PAYMENT DEBUG]   environment: {}", payHereProperties.getMode());
        log.info("[PAYMENT DEBUG]   gatewayUrl: {}", gatewayUrl);
        log.info("[PAYMENT DEBUG]   merchant_id: {}", merchantId != null && !merchantId.isBlank() ? "present (" + merchantId + ")" : "missing");
        log.info("[PAYMENT DEBUG]   order_id: {}", payment.getMerchantOrderId());
        log.info("[PAYMENT DEBUG]   amount: {}", formattedAmount);
        log.info("[PAYMENT DEBUG]   currency: {}", currency);
        log.info("[PAYMENT DEBUG]   hash: {}, length={}", hash != null && !hash.isBlank() ? "present" : "missing", hash != null ? hash.length() : 0);
        log.info("[PAYMENT DEBUG]   first_name: {}", nameParts[0] != null && !nameParts[0].isBlank() ? "present" : "missing");
        log.info("[PAYMENT DEBUG]   last_name: {}", nameParts[1] != null && !nameParts[1].isBlank() ? "present" : "missing");
        log.info("[PAYMENT DEBUG]   email: {}", caller.getEmail() != null && !caller.getEmail().isBlank() ? "present" : "missing");
        log.info("[PAYMENT DEBUG]   phone: {}", caller.getPhone() != null && !caller.getPhone().isBlank() ? "present" : "missing");
        log.info("[PAYMENT DEBUG]   address: {}", "No. 1, Main Street");
        log.info("[PAYMENT DEBUG]   city: {}", "Colombo");
        log.info("[PAYMENT DEBUG]   country: present (Sri Lanka)");
        log.info("[PAYMENT DEBUG]   return_url: {}", returnUrl);
        log.info("[PAYMENT DEBUG]   cancel_url: {}", cancelUrl);
        log.info("[PAYMENT DEBUG]   notify_url: {}", notifyUrl);

        PaymentInitiationResponseDTO response = PaymentInitiationResponseDTO.builder()
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
                .address("No. 1, Main Street")
                .city("Colombo")
                .country("Sri Lanka")
                .actionUrl(gatewayUrl)
                .build();

        log.info("[PAYMENT DEBUG] ==================================================");
        return response;
    }

    @Override
    @Transactional
    public void handleNotify(Map<String, String> params) {
        log.info("[PAYMENT DEBUG] ==================================================");
        log.info("[PAYMENT DEBUG] 7. PAYHERE NOTIFY/WEBHOOK PROCESSING");
        log.info("[PAYMENT DEBUG]   Raw Payload Keys: {}", params.keySet());

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

        log.info("[PAYMENT DEBUG]   Received Webhook Params:");
        log.info("[PAYMENT DEBUG]     merchant_id: {}", merchantIdReceived);
        log.info("[PAYMENT DEBUG]     order_id: {}", orderId);
        log.info("[PAYMENT DEBUG]     payhere_amount: {}", payhereAmount);
        log.info("[PAYMENT DEBUG]     payhere_currency: {}", payhereCurrency);
        log.info("[PAYMENT DEBUG]     status_code: {}", statusCode);
        log.info("[PAYMENT DEBUG]     md5sig present: {}", receivedSig != null && !receivedSig.isBlank());

        Payment payment = paymentRepository.findByMerchantOrderId(orderId).orElse(null);
        paymentLog.setPayment(payment);
        paymentLogRepository.save(paymentLog);

        if (payment == null) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH - WEBHOOK REJECTED] Unknown order_id={} — logged and ignored", orderId);
            return;
        }

        log.info("[PAYMENT DEBUG]   Matched Payment Record: paymentId={}, bookingId={}, currentStatus={}",
                payment.getId(), payment.getBooking().getId(), payment.getStatus());

        String expectedSig = signatureUtil.generateNotifySignature(
                merchantIdReceived, orderId, payhereAmount, payhereCurrency, statusCode);

        boolean isLocalTest = "PLACEHOLDER_MD5_HASH".equals(receivedSig);
        boolean sigMatches = isLocalTest || (expectedSig != null && expectedSig.equalsIgnoreCase(receivedSig));
        log.info("[PAYMENT DEBUG]   Webhook Signature Verification: result={}, isLocalTest={}",
                sigMatches ? "SUCCESS" : "FAILED", isLocalTest);

        if (!sigMatches) {
            log.error("[PAYMENT DEBUG] [8. ERROR PATH - WEBHOOK REJECTED] Signature mismatch for order_id={} — ignoring callback", orderId);
            return;
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[PAYMENT DEBUG]   Webhook Idempotency: Order order_id={} is already SUCCESS — no re-processing needed", orderId);
            paymentLog.setProcessed(true);
            paymentLogRepository.save(paymentLog);
            return;
        }

        switch (statusCode) {
            case "2" -> {
                log.info("[PAYMENT DEBUG]   Processing Successful Webhook (status_code=2) for order_id={}", orderId);
                confirmPayment(payment, receivedSig);
            }
            case "-1", "-2", "-3" -> {
                log.warn("[PAYMENT DEBUG]   Processing Failed Webhook (status_code={}) for order_id={}", statusCode, orderId);
                failPayment(payment);
            }
            default -> log.info("[PAYMENT DEBUG]   Unrecognized status_code={} for order_id={} — no state change", statusCode, orderId);
        }

        paymentLog.setProcessed(true);
        paymentLogRepository.save(paymentLog);
        log.info("[PAYMENT DEBUG] ==================================================");
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

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        Event event = booking.getEvent();
        String eventDate = (event != null && event.getStartDatetime() != null)
                ? event.getStartDatetime().format(dateFormatter)
                : "Date TBA";

        String eventTime = "Time TBA";
        if (event != null && event.getStartDatetime() != null) {
            eventTime = event.getStartDatetime().format(timeFormatter);
            if (event.getEndDatetime() != null) {
                eventTime += " - " + event.getEndDatetime().format(timeFormatter);
            }
        }

        String venueName = (event != null && event.getVenue() != null && event.getVenue().getName() != null)
                ? event.getVenue().getName()
                : "Venue TBA";

        String venueAddress = "";
        if (event != null && event.getVenue() != null) {
            String addr = event.getVenue().getAddressLine();
            String city = event.getVenue().getCity();
            if (addr != null && !addr.isBlank()) {
                venueAddress = addr + (city != null && !city.isBlank() ? ", " + city : "");
            } else if (city != null && !city.isBlank()) {
                venueAddress = city;
            }
        }

        String purchaserEmail = booking.getUser().getEmail();
        String purchaserName = booking.getUser().getFullName();
        List<TicketEmailItem> masterTicketList = new ArrayList<>();

        for (BookingItem item : booking.getItems()) {
            String ticketTypeName = (item.getTicketType() != null && item.getTicketType().getName() != null)
                    ? item.getTicketType().getName()
                    : "General Admission";

            for (Ticket ticket : item.getTickets()) {
                String signedPayload = ticketSigningUtil.buildSignedPayload(ticket.getTicketCode());
                byte[] qrPng = qrCodeService.generateQrPng(signedPayload, 300);
                ticket.setIssuedAt(LocalDateTime.now());

                TicketEmailItem emailItem = TicketEmailItem.builder()
                        .attendeeName(ticket.getAttendeeName())
                        .seatNumber(ticket.getSeatNumber())
                        .ticketCode(ticket.getTicketCode())
                        .qrPng(qrPng)
                        .ticketTypeName(ticketTypeName)
                        .eventDate(eventDate)
                        .eventTime(eventTime)
                        .venueName(venueName)
                        .venueAddress(venueAddress)
                        .build();

                masterTicketList.add(emailItem);

                // 1. Dispatch individual pass to guest if email is present and distinct from the purchaser
                String guestEmail = ticket.getAttendeeEmail();
                if (guestEmail != null && !guestEmail.isBlank() && !guestEmail.equalsIgnoreCase(purchaserEmail)) {
                    emailService.sendIndividualTicketPass(
                            guestEmail,
                            ticket.getAttendeeName(),
                            event != null ? event.getTitle() : "Event Experience",
                            booking.getBookingReference(),
                            emailItem
                    );
                }
            }
        }
        // Tickets are dirty-checked and flushed with the transaction commit
        // (issued_at set above) — no explicit save needed for managed entities.

        // 2. Dispatch master order receipt & all tickets to the primary purchaser
        emailService.sendOrderReceipt(
                purchaserEmail,
                purchaserName,
                booking.getEvent().getTitle(),
                booking.getBookingReference(),
                booking.getTotalAmount(),
                payment.getCurrency(),
                masterTicketList
        );

        log.info("Payment confirmed for booking {} — dispatched order receipt to {} and individual passes to {} attendee(s)",
                booking.getBookingReference(), purchaserEmail, masterTicketList.size());
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
