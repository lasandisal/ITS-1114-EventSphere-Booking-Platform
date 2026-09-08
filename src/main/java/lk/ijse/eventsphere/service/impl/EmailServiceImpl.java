package lk.ijse.eventsphere.service.impl;

import jakarta.mail.internet.MimeMessage;
import lk.ijse.eventsphere.service.EmailService;
import lk.ijse.eventsphere.service.TicketEmailItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;

    @Value("${app.mail.sender-email:${spring.mail.username:eventsphere.tickets@gmail.com}}")
    private String senderEmail;

    // =========================================================================
    // 1. MASTER ORDER RECEIPT (FOR THE PURCHASER)
    // =========================================================================
    @Override
    @Async
    public void sendOrderReceipt(String recipientEmail, String recipientName,
                                 String eventTitle, String bookingReference,
                                 BigDecimal totalAmount, String currency,
                                 List<TicketEmailItem> tickets) {
        log.info("[EMAIL] Initiating master order receipt email to {}", recipientEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            applyTransactionalHeaders(message, bookingReference);

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "EventSphere");
            helper.setReplyTo(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Booking Confirmation & Ticket Receipt: " + eventTitle + " [" + bookingReference + "]");

            String formattedAmount = (totalAmount != null) ? totalAmount.setScale(2).toPlainString() : "0.00";

            String plainText = "Hello " + recipientName + ",\n\n"
                    + "Thank you for your order! Your booking for " + eventTitle + " has been successfully confirmed.\n\n"
                    + "Booking Reference: " + bookingReference + "\n"
                    + "Total Paid: " + currency + " " + formattedAmount + "\n"
                    + "Total Tickets: " + (tickets != null ? tickets.size() : 0) + "\n\n"
                    + "All digital QR passes are included in this email and also accessible directly from your EventSphere dashboard.\n\n"
                    + "If you need assistance, please contact support.\n\n"
                    + "Best regards,\nEventSphere Team";

            TicketEmailItem firstTicket = (tickets != null && !tickets.isEmpty()) ? tickets.get(0) : null;
            String eventDate = (firstTicket != null && firstTicket.getEventDate() != null) ? firstTicket.getEventDate() : "Date TBA";
            String eventTime = (firstTicket != null && firstTicket.getEventTime() != null) ? firstTicket.getEventTime() : "Time TBA";
            String venueName = (firstTicket != null && firstTicket.getVenueName() != null) ? firstTicket.getVenueName() : "Venue TBA";
            String venueAddress = (firstTicket != null && firstTicket.getVenueAddress() != null) ? firstTicket.getVenueAddress() : "";

            Context context = new Context();
            context.setVariable("name", recipientName);
            context.setVariable("eventTitle", eventTitle);
            context.setVariable("bookingReference", bookingReference);
            context.setVariable("eventDate", eventDate);
            context.setVariable("eventTime", eventTime);
            context.setVariable("venueName", venueName);
            context.setVariable("venueAddress", venueAddress);
            context.setVariable("totalTickets", tickets != null ? tickets.size() : 0);
            context.setVariable("currency", currency != null ? currency : "LKR");
            context.setVariable("formattedAmount", formattedAmount);
            context.setVariable("tickets", tickets != null ? tickets : List.of());

            String htmlBody = templateEngine.process("mail/order-receipt", context);
            helper.setText(plainText, htmlBody);

            if (tickets != null) {
                for (int i = 0; i < tickets.size(); i++) {
                    TicketEmailItem ticket = tickets.get(i);
                    if (ticket.getQrPng() != null && ticket.getQrPng().length > 0) {
                        helper.addInline("receipt_qr" + i, new ByteArrayResource(ticket.getQrPng()), "image/png");
                    }
                }
            }

            mailSender.send(message);
            log.info("[EMAIL SUCCESS] Master order receipt successfully sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("[EMAIL ERROR] Failed to send order receipt to {} for booking {}: {}",
                    recipientEmail, bookingReference, e.getMessage(), e);
        }
    }

    // =========================================================================
    // 2. INDIVIDUAL TICKET PASS (FOR GUEST ATTENDEES)
    // =========================================================================
    @Override
    @Async
    public void sendIndividualTicketPass(String attendeeEmail, String attendeeName,
                                         String eventTitle, String bookingReference,
                                         TicketEmailItem ticket) {
        log.info("[EMAIL] Initiating individual ticket pass to attendee {}", attendeeEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            applyTransactionalHeaders(message, bookingReference);

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "EventSphere");
            helper.setReplyTo(senderEmail);
            helper.setTo(attendeeEmail);
            helper.setSubject("Your Admission Ticket: " + eventTitle);

            String eventDate = (ticket != null && ticket.getEventDate() != null) ? ticket.getEventDate() : "Date TBA";
            String eventTime = (ticket != null && ticket.getEventTime() != null) ? ticket.getEventTime() : "Time TBA";
            String venueName = (ticket != null && ticket.getVenueName() != null) ? ticket.getVenueName() : "Venue TBA";
            String venueAddress = (ticket != null && ticket.getVenueAddress() != null) ? ticket.getVenueAddress() : "";
            String ticketTier = (ticket != null && ticket.getTicketTypeName() != null) ? ticket.getTicketTypeName() : "General Admission";

            String plainText = "Hello " + attendeeName + ",\n\n"
                    + "Here is your official digital admission ticket for " + eventTitle + "!\n\n"
                    + "Event: " + eventTitle + "\n"
                    + "Date: " + eventDate + "\n"
                    + "Time: " + eventTime + "\n"
                    + "Venue: " + venueName + "\n"
                    + "Ticket Tier: " + ticketTier + "\n"
                    + "Attendee Name: " + (ticket != null ? ticket.getAttendeeName() : attendeeName) + "\n"
                    + "Ticket Code: " + (ticket != null ? ticket.getTicketCode() : "") + "\n"
                    + (ticket != null && ticket.getSeatNumber() != null && !ticket.getSeatNumber().isBlank() ? "Seat: " + ticket.getSeatNumber() + "\n" : "")
                    + "Booking Reference: " + bookingReference + "\n\n"
                    + "Please present your QR code at the venue entrance for scanning.\n\n"
                    + "We look forward to seeing you!\nEventSphere Team";

            Context context = new Context();
            context.setVariable("name", attendeeName);
            context.setVariable("eventTitle", eventTitle);
            context.setVariable("bookingReference", bookingReference);
            context.setVariable("eventDate", eventDate);
            context.setVariable("eventTime", eventTime);
            context.setVariable("venueName", venueName);
            context.setVariable("venueAddress", venueAddress);
            context.setVariable("ticketTier", ticketTier);
            context.setVariable("ticket", ticket);

            String htmlBody = templateEngine.process("mail/ticket-pass", context);
            helper.setText(plainText, htmlBody);

            if (ticket != null && ticket.getQrPng() != null && ticket.getQrPng().length > 0) {
                helper.addInline("guest_qr", new ByteArrayResource(ticket.getQrPng()), "image/png");
            }

            mailSender.send(message);
            log.info("[EMAIL SUCCESS] Individual ticket pass successfully sent to attendee {}", attendeeEmail);
        } catch (Exception e) {
            log.error("[EMAIL ERROR] Failed to send guest pass to {} for booking {}: {}",
                    attendeeEmail, bookingReference, e.getMessage(), e);
        }
    }

    // =========================================================================
    // 3. EMAIL VERIFICATION OTP (FOR REGISTRATION & SIGN-IN)
    // =========================================================================
    @Override
    @Async
    public void sendVerificationOtpEmail(String recipientEmail, String recipientName, String otp) {
        log.info("[EMAIL] Sending verification OTP to {}", recipientEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            applyTransactionalHeaders(message, "VERIFY-" + otp);

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "EventSphere");
            helper.setReplyTo(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("EventSphere Account Verification Code");

            String plainText = "Hello " + recipientName + ",\n\n"
                    + "Your EventSphere email verification code is:\n\n"
                    + "   " + otp + "\n\n"
                    + "This code will expire in 10 minutes.\n\n"
                    + "If you did not request this code, please ignore this email.\n\n"
                    + "Best regards,\nEventSphere Team";

            Context context = new Context();
            context.setVariable("name", recipientName);
            context.setVariable("otp", otp);

            String htmlBody = templateEngine.process("mail/otp-verification", context);
            helper.setText(plainText, htmlBody);

            mailSender.send(message);
            log.info("[EMAIL SUCCESS] Verification OTP successfully sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("[EMAIL ERROR] Failed to send verification OTP to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetOtpEmail(String recipientEmail, String recipientName, String otp) {
        log.info("[EMAIL] Sending password reset OTP to {}", recipientEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            applyTransactionalHeaders(message, "RESET-" + otp);

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "EventSphere");
            helper.setReplyTo(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("EventSphere Password Reset Code");

            String plainText = "Hello " + recipientName + ",\n\n"
                    + "You requested to reset your password for EventSphere.\n\n"
                    + "Your password reset verification code is:\n\n"
                    + "   " + otp + "\n\n"
                    + "This code will expire in 10 minutes.\n\n"
                    + "If you did not request a password reset, you can safely ignore this email. Your password will remain unchanged.\n\n"
                    + "Best regards,\nEventSphere Security Team";

            Context context = new Context();
            context.setVariable("name", recipientName);
            context.setVariable("otp", otp);

            String htmlBody = templateEngine.process("mail/password-reset-otp", context);
            helper.setText(plainText, htmlBody);

            mailSender.send(message);
            log.info("[EMAIL SUCCESS] Password reset OTP successfully sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("[EMAIL ERROR] Failed to send password reset OTP to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    // =========================================================================
    // LEGACY COMPATIBILITY
    // =========================================================================
    @Override
    @Async
    public void sendBookingConfirmation(String recipientEmail, String recipientName,
                                        String eventTitle, String bookingReference,
                                        List<TicketEmailItem> tickets) {
        sendOrderReceipt(recipientEmail, recipientName, eventTitle, bookingReference, BigDecimal.ZERO, "LKR", tickets);
    }

    private void applyTransactionalHeaders(MimeMessage message, String bookingReference) throws Exception {
        message.setHeader("X-Priority", "3");
        message.setHeader("Importance", "Normal");
        message.setHeader("Auto-Submitted", "auto-generated");
        message.setHeader("X-Auto-Response-Suppress", "All");
        if (bookingReference != null && !bookingReference.isBlank()) {
            message.setHeader("X-Booking-Reference", bookingReference);
        }
    }
}