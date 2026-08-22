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

    @Value("${spring.mail.username:eventsphere.tickets@gmail.com}")
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
    private String buildMasterReceiptHtml(String name, String eventTitle, String bookingReference,
                                          BigDecimal totalAmount, String currency, List<TicketEmailItem> tickets) {
        String formattedAmount = (totalAmount != null) ? totalAmount.setScale(2).toPlainString() : "0.00";
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>")
                .append("<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px;'>")
                .append("<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);'>")

                // Header Banner
                .append("<tr><td style='background: #7d4f50; padding: 28px 24px; text-align: center; color: #ffffff;'>")
                .append("<h1 style='margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;'>EventSphere</h1>")
                .append("<p style='margin: 6px 0 0 0; font-size: 14px; color: #f2d6d7;'>Order Receipt &amp; Admission Passes</p>")
                .append("</td></tr>")

                // Content
                .append("<tr><td style='padding: 28px 32px 16px 32px;'>")
                .append("<p style='font-size: 16px; color: #1e293b; margin: 0 0 12px 0;'>Hello <b>").append(escape(name)).append("</b>,</p>")
                .append("<p style='font-size: 14px; color: #475569; line-height: 1.6; margin: 0 0 20px 0;'>Thank you for booking with EventSphere! Your order for <b>").append(escape(eventTitle)).append("</b> is confirmed. Below are your booking details and admission passes.</p>")

                // Receipt Box
                .append("<div style='background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin-bottom: 24px; font-size: 14px; color: #334155;'>")
                .append("<table width='100%' border='0' cellpadding='0' cellspacing='0'>")
                .append("<tr><td style='padding: 4px 0; color: #64748b;'>Booking Reference:</td><td align='right' style='padding: 4px 0;'><strong style='font-family: monospace; font-size: 13px;'>").append(escape(bookingReference)).append("</strong></td></tr>")
                .append("<tr><td style='padding: 4px 0; color: #64748b;'>Event:</td><td align='right' style='padding: 4px 0;'><strong>").append(escape(eventTitle)).append("</strong></td></tr>")
                .append("<tr><td style='padding: 4px 0; color: #64748b;'>Total Tickets:</td><td align='right' style='padding: 4px 0;'><strong>").append(tickets.size()).append("</strong></td></tr>")
                .append("<tr><td style='padding: 8px 0 0 0; border-top: 1px dashed #cbd5e1; color: #1e293b; font-weight: 600;'>Total Paid:</td><td align='right' style='padding: 8px 0 0 0; border-top: 1px dashed #cbd5e1; color: #7d4f50; font-size: 16px; font-weight: 700;'>").append(escape(currency)).append(" ").append(formattedAmount).append("</td></tr>")
                .append("</table>")
                .append("</div>")

                .append("<h3 style='font-size: 16px; font-weight: 600; margin: 0 0 16px 0; color: #1e293b;'>Your Digital Admission Passes</h3>")
                .append("</td></tr>");

        // Tickets List
        for (int i = 0; i < tickets.size(); i++) {
            TicketEmailItem t = tickets.get(i);
            String cid = "receipt_qr" + i;
            html.append("<tr><td style='padding: 0 32px 16px 32px;'>")
                    .append("<div style='border: 1px solid #e2e8f0; border-radius: 10px; padding: 18px; background-color: #ffffff;'>")
                    .append("<table width='100%' border='0' cellpadding='0' cellspacing='0'>")
                    .append("<tr><td valign='top' style='font-size: 14px; color: #334155;'>")
                    .append("<p style='margin: 0 0 6px 0;'><span style='color: #64748b;'>Attendee:</span> <b>").append(escape(t.getAttendeeName())).append("</b></p>");
            if (t.getSeatNumber() != null && !t.getSeatNumber().isBlank()) {
                html.append("<p style='margin: 0 0 6px 0;'><span style='color: #64748b;'>Seat / Zone:</span> <b>").append(escape(t.getSeatNumber())).append("</b></p>");
            }
            html.append("<p style='margin: 0;'><span style='color: #64748b;'>Ticket Code:</span> <code style='font-size: 12px; background: #f1f5f9; padding: 2px 6px; border-radius: 4px;'>").append(escape(t.getTicketCode())).append("</code></p>")
                    .append("</td></tr>")
                    .append("<tr><td align='center' style='padding-top: 14px;'>")
                    .append("<img src='cid:").append(cid).append("' width='160' height='160' alt='QR Code Pass' style='display: block; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 6px;' />")
                    .append("<p style='font-size: 12px; color: #64748b; margin: 6px 0 0 0;'>Scan at venue entrance</p>")
                    .append("</td></tr></table>")
                    .append("</div></td></tr>");
        }

        // Footer
        html.append("<tr><td style='padding: 24px 32px; background-color: #fafafa; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; line-height: 1.6;'>")
                .append("<p style='margin: 0 0 6px 0;'><strong>EventSphere Ticketing Platform</strong></p>")
                .append("<p style='margin: 0 0 6px 0;'>This is an official transactional receipt for your booking.</p>")
                .append("<p style='margin: 0; color: #94a3b8;'>Please present your digital QR code upon arrival at the venue.</p>")
                .append("</td></tr></table></body></html>");

        return html.toString();
    }

    private String buildIndividualTicketHtml(String name, String eventTitle, String bookingReference, TicketEmailItem ticket) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 550px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);'>"
                + "<tr><td style='background: #7d4f50; padding: 26px 24px; text-align: center; color: #ffffff;'>"
                + "<h1 style='margin: 0; font-size: 20px; font-weight: 700;'>Your Admission Pass</h1>"
                + "<p style='margin: 4px 0 0 0; font-size: 14px; color: #f2d6d7;'>EventSphere Digital Ticket</p>"
                + "</td></tr>"
                + "<tr><td style='padding: 28px 32px; text-align: center;'>"
                + "<p style='font-size: 16px; color: #1e293b; margin: 0 0 8px 0;'>Hello <b>" + escape(name) + "</b>,</p>"
                + "<p style='font-size: 14px; color: #475569; line-height: 1.5; margin: 0 0 20px 0;'>Here is your official digital admission ticket for <b>" + escape(eventTitle) + "</b>.</p>"
                + "<div style='border: 2px dashed #7d4f50; border-radius: 10px; padding: 20px; background: #fafafa; display: inline-block; width: 85%; box-sizing: border-box;'>"
                + "<img src='cid:guest_qr' width='180' height='180' alt='Admission QR Code' style='display: block; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 6px; background: #ffffff;' />"
                + "<p style='font-size: 12px; color: #64748b; margin: 10px 0 14px 0;'>Scan at venue entrance</p>"
                + "<div style='font-size: 13px; color: #334155; text-align: left; background: #ffffff; padding: 12px 14px; border-radius: 6px; border: 1px solid #e2e8f0; line-height: 1.6;'>"
                + "<div><span style='color: #64748b;'>Attendee:</span> <b>" + escape(ticket.getAttendeeName()) + "</b></div>"
                + (ticket.getSeatNumber() != null && !ticket.getSeatNumber().isBlank() ? "<div><span style='color: #64748b;'>Seat / Zone:</span> <b>" + escape(ticket.getSeatNumber()) + "</b></div>" : "")
                + "<div><span style='color: #64748b;'>Ticket Code:</span> <code style='font-size: 12px; background: #f1f5f9; padding: 2px 4px; border-radius: 3px;'>" + escape(ticket.getTicketCode()) + "</code></div>"
                + "<div style='font-size: 11px; color: #94a3b8; margin-top: 6px; border-top: 1px solid #f1f5f9; padding-top: 6px;'>Booking Reference: " + escape(bookingReference) + "</div>"
                + "</div>"
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding: 20px 32px; background-color: #fafafa; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; line-height: 1.5;'>"
                + "<p style='margin: 0 0 4px 0;'><strong>EventSphere Ticketing Platform</strong></p>"
                + "<p style='margin: 0;'>Please have this digital QR pass ready on your device upon arrival at the venue.</p>"
                + "</td></tr></table></body></html>";
    }

    private void applyTransactionalHeaders(MimeMessage message, String bookingReference) throws Exception {
        message.setHeader("X-Priority", "3");
        message.setHeader("Importance", "Normal");
        if (bookingReference != null && !bookingReference.isBlank()) {
            message.setHeader("X-Booking-Reference", bookingReference);
        }
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}