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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

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
                    + "Total Tickets: " + tickets.size() + "\n\n"
                    + "All digital QR passes are included in this email and also accessible directly from your EventSphere dashboard.\n\n"
                    + "If you need assistance, please contact support.\n\n"
                    + "Best regards,\nEventSphere Team";

            String htmlBody = buildMasterReceiptHtml(recipientName, eventTitle, bookingReference, totalAmount, currency, tickets);
            helper.setText(plainText, htmlBody);

            for (int i = 0; i < tickets.size(); i++) {
                TicketEmailItem ticket = tickets.get(i);
                if (ticket.getQrPng() != null && ticket.getQrPng().length > 0) {
                    helper.addInline("receipt_qr" + i, new ByteArrayResource(ticket.getQrPng()), "image/png");
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

            String plainText = "Hello " + attendeeName + ",\n\n"
                    + "Here is your official digital admission ticket for " + eventTitle + "!\n\n"
                    + "Event: " + eventTitle + "\n"
                    + "Date: " + (ticket.getEventDate() != null ? ticket.getEventDate() : "TBA") + "\n"
                    + "Time: " + (ticket.getEventTime() != null ? ticket.getEventTime() : "TBA") + "\n"
                    + "Venue: " + (ticket.getVenueName() != null ? ticket.getVenueName() : "TBA") + "\n"
                    + "Ticket Tier: " + (ticket.getTicketTypeName() != null ? ticket.getTicketTypeName() : "General Admission") + "\n"
                    + "Attendee Name: " + ticket.getAttendeeName() + "\n"
                    + "Ticket Code: " + ticket.getTicketCode() + "\n"
                    + (ticket.getSeatNumber() != null && !ticket.getSeatNumber().isBlank() ? "Seat: " + ticket.getSeatNumber() + "\n" : "")
                    + "Booking Reference: " + bookingReference + "\n\n"
                    + "Please present your QR code at the venue entrance for scanning.\n\n"
                    + "We look forward to seeing you!\nEventSphere Team";

            String htmlBody = buildIndividualTicketHtml(attendeeName, eventTitle, bookingReference, ticket);
            helper.setText(plainText, htmlBody);

            if (ticket.getQrPng() != null && ticket.getQrPng().length > 0) {
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

            String htmlBody = buildOtpVerificationHtml(recipientName, otp);
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

            String htmlBody = buildPasswordResetOtpHtml(recipientName, otp);
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

    // =========================================================================
    // HTML TEMPLATES
    // =========================================================================
    private String buildOtpVerificationHtml(String name, String otp) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 550px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.2);'>"
                + "<tr><td style='background: linear-gradient(135deg, #090d16 0%, #1e1b4b 55%, #4f46e5 100%); padding: 32px 24px; text-align: center; color: #ffffff;'>"
                + "<div style='display: inline-block; padding: 4px 12px; background: rgba(99, 102, 241, 0.25); border: 1px solid rgba(165, 180, 252, 0.4); border-radius: 20px; font-size: 11px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; color: #c7d2fe; margin-bottom: 8px;'>ACCOUNT VERIFICATION</div>"
                + "<h1 style='margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;'>EventSphere</h1>"
                + "<p style='margin: 6px 0 0 0; font-size: 14px; color: #a5b4fc;'>Confirm Your Email Address</p>"
                + "</td></tr>"
                + "<tr><td style='padding: 32px 32px 24px 32px; text-align: center;'>"
                + "<p style='font-size: 16px; color: #0f172a; margin: 0 0 12px 0;'>Hello <b>" + escape(name) + "</b>,</p>"
                + "<p style='font-size: 14px; color: #475569; line-height: 1.6; margin: 0 0 24px 0;'>Thank you for creating an account on EventSphere. Please use the 6-digit verification code below to activate your account and access event booking:</p>"
                + "<div style='background: #f8fafc; border: 2px dashed #6366f1; border-radius: 14px; padding: 22px; margin: 0 auto 24px auto; display: inline-block; min-width: 260px; box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);'>"
                + "<div style='font-size: 11px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; color: #6366f1; margin-bottom: 8px;'>VERIFICATION CODE</div>"
                + "<span style='font-family: \"Courier New\", Courier, monospace; font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #1e1b4b; display: inline-block; padding-left: 10px;'>" + escape(otp) + "</span>"
                + "</div>"
                + "<p style='font-size: 13px; color: #64748b; margin: 0 0 20px 0;'>⏱️ This code will expire in <strong>10 minutes</strong> and can only be used once.</p>"
                + "<div style='background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 10px; padding: 14px 18px; font-size: 12px; color: #1e40af; text-align: left; line-height: 1.5;'>"
                + "<strong style='color: #1e3a8a;'>🛡️ Security Tip:</strong> Never share this verification code with anyone. EventSphere staff will never request your code."
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding: 24px 32px; background-color: #fafafa; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; line-height: 1.6;'>"
                + "<p style='margin: 0 0 4px 0;'><strong>EventSphere Security &amp; Identity</strong></p>"
                + "<p style='margin: 0; color: #94a3b8;'>If you did not register for an EventSphere account, you can safely ignore this email.</p>"
                + "</td></tr></table></body></html>";
    }

    private String buildPasswordResetOtpHtml(String name, String otp) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 550px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.2);'>"
                + "<tr><td style='background: linear-gradient(135deg, #090d16 0%, #1e1b4b 55%, #4f46e5 100%); padding: 32px 24px; text-align: center; color: #ffffff;'>"
                + "<div style='display: inline-block; padding: 4px 12px; background: rgba(236, 72, 153, 0.25); border: 1px solid rgba(244, 114, 182, 0.4); border-radius: 20px; font-size: 11px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; color: #fbcfe8; margin-bottom: 8px;'>SECURITY ALERT</div>"
                + "<h1 style='margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;'>EventSphere</h1>"
                + "<p style='margin: 6px 0 0 0; font-size: 14px; color: #a5b4fc;'>Password Reset Verification</p>"
                + "</td></tr>"
                + "<tr><td style='padding: 32px 32px 24px 32px; text-align: center;'>"
                + "<p style='font-size: 16px; color: #0f172a; margin: 0 0 12px 0;'>Hello <b>" + escape(name) + "</b>,</p>"
                + "<p style='font-size: 14px; color: #475569; line-height: 1.6; margin: 0 0 24px 0;'>We received a request to reset your EventSphere account password. Please use the 6-digit verification code below to set a new password:</p>"
                + "<div style='background: #f8fafc; border: 2px dashed #ec4899; border-radius: 14px; padding: 22px; margin: 0 auto 24px auto; display: inline-block; min-width: 260px; box-shadow: 0 4px 16px rgba(236, 72, 153, 0.08);'>"
                + "<div style='font-size: 11px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; color: #db2777; margin-bottom: 8px;'>PASSWORD RESET CODE</div>"
                + "<span style='font-family: \"Courier New\", Courier, monospace; font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #831843; display: inline-block; padding-left: 10px;'>" + escape(otp) + "</span>"
                + "</div>"
                + "<p style='font-size: 13px; color: #64748b; margin: 0 0 20px 0;'>⏱️ This password reset code will expire in <strong>10 minutes</strong>.</p>"
                + "<div style='background: #fffbeb; border: 1px solid #fef3c7; border-radius: 10px; padding: 14px 18px; font-size: 12px; color: #92400e; text-align: left; line-height: 1.5;'>"
                + "<strong style='color: #78350f;'>⚠️ Security Alert:</strong> If you did not initiate this password reset, please ignore this email or review your account security immediately."
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding: 24px 32px; background-color: #fafafa; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; line-height: 1.6;'>"
                + "<p style='margin: 0 0 4px 0;'><strong>EventSphere Security &amp; Identity</strong></p>"
                + "<p style='margin: 0; color: #94a3b8;'>Secure access to your tickets and events.</p>"
                + "</td></tr></table></body></html>";
    }

        private String buildMasterReceiptHtml(String name, String eventTitle, String bookingReference,
                                          BigDecimal totalAmount, String currency, List<TicketEmailItem> tickets) {
        String formattedAmount = (totalAmount != null) ? totalAmount.setScale(2).toPlainString() : "0.00";
        TicketEmailItem firstTicket = (tickets != null && !tickets.isEmpty()) ? tickets.get(0) : null;
        String eventDate = (firstTicket != null && firstTicket.getEventDate() != null) ? firstTicket.getEventDate() : "Date TBA";
        String eventTime = (firstTicket != null && firstTicket.getEventTime() != null) ? firstTicket.getEventTime() : "Time TBA";
        String venueName = (firstTicket != null && firstTicket.getVenueName() != null) ? firstTicket.getVenueName() : "Venue TBA";
        String venueAddress = (firstTicket != null && firstTicket.getVenueAddress() != null) ? firstTicket.getVenueAddress() : "";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>")
                .append("<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px;'>")
                .append("<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.2);'>")

                // Header Banner
                .append("<tr><td style='background: linear-gradient(135deg, #090d16 0%, #1e1b4b 55%, #4f46e5 100%); padding: 32px 24px; text-align: center; color: #ffffff;'>")
                .append("<div style='display: inline-block; padding: 4px 12px; background: rgba(99, 102, 241, 0.25); border: 1px solid rgba(165, 180, 252, 0.4); border-radius: 20px; font-size: 11px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; color: #c7d2fe; margin-bottom: 8px;'>OFFICIAL RECEIPT &amp; PASSES</div>")
                .append("<h1 style='margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;'>EventSphere</h1>")
                .append("<p style='margin: 6px 0 0 0; font-size: 14px; color: #a5b4fc;'>Booking Confirmation &amp; Admission Passes</p>")
                .append("</td></tr>")

                // Content
                .append("<tr><td style='padding: 28px 32px 16px 32px;'>")
                .append("<p style='font-size: 16px; color: #0f172a; margin: 0 0 8px 0;'>Hello <b>").append(escape(name)).append("</b>,</p>")
                .append("<p style='font-size: 14px; color: #475569; line-height: 1.6; margin: 0 0 20px 0;'>Thank you for booking with EventSphere! Your order for <b>").append(escape(eventTitle)).append("</b> is confirmed. Below are your event details, payment summary, and digital admission passes.</p>")

                // Event & Receipt Summary Box
                .append("<div style='background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 24px; font-size: 13px; color: #334155;'>")
                .append("<div style='font-size: 16px; font-weight: 700; color: #0f172a; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #e2e8f0;'>").append(escape(eventTitle)).append("</div>")
                .append("<table width='100%' border='0' cellpadding='0' cellspacing='0'>")
                .append("<tr><td style='padding: 5px 0; color: #64748b;'>📅 Date:</td><td align='right' style='padding: 5px 0; font-weight: 600; color: #0f172a;'>").append(escape(eventDate)).append("</td></tr>")
                .append("<tr><td style='padding: 5px 0; color: #64748b;'>⏰ Time:</td><td align='right' style='padding: 5px 0; font-weight: 600; color: #0f172a;'>").append(escape(eventTime)).append("</td></tr>")
                .append("<tr><td style='padding: 5px 0; color: #64748b;'>📍 Venue:</td><td align='right' style='padding: 5px 0; font-weight: 600; color: #0f172a;'>").append(escape(venueName))
                .append(!venueAddress.isBlank() ? " <span style='font-weight: 400; color: #64748b;'>(" + escape(venueAddress) + ")</span>" : "").append("</td></tr>")
                .append("<tr><td style='padding: 5px 0; color: #64748b;'>🔖 Booking Reference:</td><td align='right' style='padding: 5px 0;'><code style='font-size: 12px; background: #e0e7ff; color: #3730a3; padding: 2px 6px; border-radius: 4px; font-weight: 700;'>").append(escape(bookingReference)).append("</code></td></tr>")
                .append("<tr><td style='padding: 5px 0; color: #64748b;'>🎟️ Total Tickets:</td><td align='right' style='padding: 5px 0; font-weight: 600; color: #0f172a;'>").append(tickets != null ? tickets.size() : 0).append("</td></tr>")
                .append("<tr><td style='padding: 10px 0 0 0; border-top: 1px dashed #cbd5e1; color: #0f172a; font-weight: 700; font-size: 14px;'>Total Paid:</td><td align='right' style='padding: 10px 0 0 0; border-top: 1px dashed #cbd5e1; color: #4338ca; font-size: 18px; font-weight: 800;'>").append(escape(currency)).append(" ").append(formattedAmount).append("</td></tr>")
                .append("</table>")
                .append("</div>")

                .append("<h3 style='font-size: 16px; font-weight: 700; margin: 0 0 16px 0; color: #0f172a;'>Digital Admission Passes</h3>")
                .append("</td></tr>");

        // Tickets List
        if (tickets != null) {
            for (int i = 0; i < tickets.size(); i++) {
                TicketEmailItem t = tickets.get(i);
                String cid = "receipt_qr" + i;
                html.append("<tr><td style='padding: 0 32px 20px 32px;'>")
                        .append("<div style='border: 2px dashed #6366f1; border-radius: 12px; padding: 20px; background-color: #ffffff; box-shadow: 0 4px 12px rgba(99, 102, 241, 0.06);'>")
                        .append("<table width='100%' border='0' cellpadding='0' cellspacing='0'>")
                        .append("<tr>")
                        .append("<td width='55%' valign='top' style='font-size: 13px; color: #334155;'>")
                        .append("<div style='margin-bottom: 8px;'><span style='background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; padding: 2px 8px; border-radius: 12px; font-size: 10px; font-weight: 700;'>✓ VALID PASS</span></div>")
                        .append("<p style='margin: 0 0 6px 0;'><span style='color: #64748b;'>Attendee:</span> <b style='color: #0f172a;'>").append(escape(t.getAttendeeName())).append("</b></p>")
                        .append("<p style='margin: 0 0 6px 0;'><span style='color: #64748b;'>Ticket Tier:</span> <b style='color: #4338ca;'>").append(escape(t.getTicketTypeName() != null ? t.getTicketTypeName() : "General Admission")).append("</b></p>");
                if (t.getSeatNumber() != null && !t.getSeatNumber().isBlank()) {
                    html.append("<p style='margin: 0 0 6px 0;'><span style='color: #64748b;'>Seat / Zone:</span> <b>").append(escape(t.getSeatNumber())).append("</b></p>");
                }
                html.append("<p style='margin: 0 0 6px 0;'><span style='color: #64748b;'>Ticket Code:</span> <code style='font-size: 11px; background: #e0e7ff; color: #3730a3; padding: 2px 5px; border-radius: 4px; font-weight: 600;'>").append(escape(t.getTicketCode())).append("</code></p>")
                        .append("<p style='margin: 0;'><span style='color: #94a3b8; font-size: 11px;'>Booking Ref: ").append(escape(bookingReference)).append("</span></p>")
                        .append("</td>")
                        .append("<td width='45%' align='center' valign='middle' style='padding-left: 12px;'>")
                        .append("<img src='cid:").append(cid).append("' width='140' height='140' alt='QR Code Pass' style='display: block; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 6px; background: #ffffff;' />")
                        .append("<p style='font-size: 11px; color: #64748b; margin: 6px 0 0 0; text-align: center;'>Scan at entrance</p>")
                        .append("</td></tr></table>")
                        .append("</div></td></tr>");
            }
        }

        // Footer
        html.append("<tr><td style='padding: 24px 32px; background-color: #fafafa; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; line-height: 1.6;'>")
                .append("<p style='margin: 0 0 4px 0;'><strong>EventSphere Ticketing Platform</strong></p>")
                .append("<p style='margin: 0 0 4px 0;'>This is an official transactional receipt and entry pass for your booking.</p>")
                .append("<p style='margin: 0; color: #94a3b8;'>Please present your digital QR code upon arrival at the venue.</p>")
                .append("</td></tr></table></body></html>");

        return html.toString();
    }

    private String buildIndividualTicketHtml(String name, String eventTitle, String bookingReference, TicketEmailItem ticket) {
        String eventDate = (ticket != null && ticket.getEventDate() != null) ? ticket.getEventDate() : "Date TBA";
        String eventTime = (ticket != null && ticket.getEventTime() != null) ? ticket.getEventTime() : "Time TBA";
        String venueName = (ticket != null && ticket.getVenueName() != null) ? ticket.getVenueName() : "Venue TBA";
        String venueAddress = (ticket != null && ticket.getVenueAddress() != null) ? ticket.getVenueAddress() : "";
        String ticketTier = (ticket != null && ticket.getTicketTypeName() != null) ? ticket.getTicketTypeName() : "General Admission";

        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 550px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.2);'>"
                // Header
                + "<tr><td style='background: linear-gradient(135deg, #090d16 0%, #1e1b4b 55%, #4f46e5 100%); padding: 32px 24px; text-align: center; color: #ffffff;'>"
                + "<div style='display: inline-block; padding: 4px 12px; background: rgba(99, 102, 241, 0.25); border: 1px solid rgba(165, 180, 252, 0.4); border-radius: 20px; font-size: 11px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; color: #c7d2fe; margin-bottom: 8px;'>OFFICIAL DIGITAL PASS</div>"
                + "<h1 style='margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;'>EventSphere</h1>"
                + "<p style='margin: 6px 0 0 0; font-size: 14px; color: #a5b4fc;'>Verified Admission Pass</p>"
                + "</td></tr>"

                // Greeting
                + "<tr><td style='padding: 28px 32px 20px 32px; text-align: center;'>"
                + "<p style='font-size: 16px; color: #0f172a; margin: 0 0 8px 0;'>Hello <b>" + escape(name) + "</b>,</p>"
                + "<p style='font-size: 14px; color: #475569; line-height: 1.5; margin: 0 0 20px 0;'>Here is your official digital admission ticket for:</p>"

                // Event Details Card
                + "<div style='background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px 20px; margin-bottom: 24px; text-align: left;'>"
                + "<div style='font-size: 17px; font-weight: 700; color: #0f172a; margin-bottom: 12px;'>" + escape(eventTitle) + "</div>"
                + "<table width='100%' border='0' cellpadding='0' cellspacing='0' style='font-size: 13px;'>"
                + "<tr>"
                + "<td width='50%' valign='top' style='padding: 6px 8px 6px 0;'>"
                + "<div style='color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;'>📅 Date</div>"
                + "<div style='font-weight: 600; color: #0f172a; margin-top: 2px;'>" + escape(eventDate) + "</div>"
                + "</td>"
                + "<td width='50%' valign='top' style='padding: 6px 0 6px 8px;'>"
                + "<div style='color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;'>⏰ Time</div>"
                + "<div style='font-weight: 600; color: #0f172a; margin-top: 2px;'>" + escape(eventTime) + "</div>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td width='50%' valign='top' style='padding: 8px 8px 0 0; border-top: 1px solid #e2e8f0;'>"
                + "<div style='color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;'>📍 Venue</div>"
                + "<div style='font-weight: 600; color: #0f172a; margin-top: 2px;'>" + escape(venueName)
                + (!venueAddress.isBlank() ? "<br><span style='font-size: 11px; font-weight: 400; color: #64748b;'>" + escape(venueAddress) + "</span>" : "") + "</div>"
                + "</td>"
                + "<td width='50%' valign='top' style='padding: 8px 0 0 8px; border-top: 1px solid #e2e8f0;'>"
                + "<div style='color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;'>🎟️ Ticket Tier</div>"
                + "<div style='font-weight: 600; color: #4338ca; margin-top: 2px;'>" + escape(ticketTier) + "</div>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</div>"

                // QR Code Pass Box
                + "<div style='border: 2px dashed #6366f1; border-radius: 14px; padding: 22px; background: #ffffff; box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08); margin: 0 auto 20px auto; max-width: 380px; box-sizing: border-box;'>"
                + "<div style='text-align: center; margin-bottom: 12px;'>"
                + "<span style='background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase;'>✓ VALID ADMISSION PASS</span>"
                + "</div>"
                + "<img src='cid:guest_qr' width='180' height='180' alt='Admission QR Code' style='display: block; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 10px; padding: 8px; background: #ffffff;' />"
                + "<p style='font-size: 12px; color: #64748b; margin: 10px 0 16px 0; text-align: center;'>Scan at venue entrance for admittance</p>"
                + "<div style='font-size: 13px; color: #334155; text-align: left; background: #f8fafc; padding: 12px 14px; border-radius: 8px; border: 1px solid #e2e8f0; line-height: 1.6;'>"
                + "<div><span style='color: #64748b;'>Attendee:</span> <b style='color: #0f172a;'>" + escape(ticket.getAttendeeName()) + "</b></div>"
                + (ticket.getSeatNumber() != null && !ticket.getSeatNumber().isBlank() ? "<div><span style='color: #64748b;'>Seat / Zone:</span> <b>" + escape(ticket.getSeatNumber()) + "</b></div>" : "")
                + "<div><span style='color: #64748b;'>Ticket Code:</span> <code style='font-size: 12px; background: #e0e7ff; color: #3730a3; padding: 2px 6px; border-radius: 4px; font-weight: 600;'>" + escape(ticket.getTicketCode()) + "</code></div>"
                + "<div style='font-size: 11px; color: #94a3b8; margin-top: 6px; border-top: 1px solid #e2e8f0; padding-top: 6px;'>Booking Reference: <strong style='color: #64748b;'>" + escape(bookingReference) + "</strong></div>"
                + "</div>"
                + "</div>"

                + "</td></tr>"
                // Footer
                + "<tr><td style='padding: 20px 32px; background-color: #fafafa; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; line-height: 1.5;'>"
                + "<p style='margin: 0 0 4px 0;'><strong>EventSphere Ticketing Platform</strong></p>"
                + "<p style='margin: 0; color: #94a3b8;'>Please have this digital QR pass ready on your device upon arrival. Gates open 30 minutes prior to event start.</p>"
                + "</td></tr></table></body></html>";
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

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}