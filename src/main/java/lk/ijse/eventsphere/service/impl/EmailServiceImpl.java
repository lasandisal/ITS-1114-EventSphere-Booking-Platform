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
            applyAntiSpamHeaders(message);

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "EventSphere");
            helper.setReplyTo(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Order Receipt: " + eventTitle + " [" + bookingReference + "]");

            String formattedAmount = (totalAmount != null) ? totalAmount.setScale(2).toPlainString() : "0.00";

            String plainText = "Hello " + recipientName + ",\n\n"
                    + "Your payment for " + eventTitle + " was successful.\n"
                    + "Booking Reference: " + bookingReference + "\n"
                    + "Total Paid: " + currency + " " + formattedAmount + "\n"
                    + "Total Tickets: " + tickets.size() + "\n\n"
                    + "All digital QR passes are included below and accessible in your EventSphere dashboard.\n\n"
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
            applyAntiSpamHeaders(message);

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "EventSphere");
            helper.setReplyTo(senderEmail);
            helper.setTo(attendeeEmail);
            helper.setSubject("Your Admission Pass: " + eventTitle);

            String plainText = "Hello " + attendeeName + ",\n\n"
                    + "You have a digital ticket for " + eventTitle + "!\n"
                    + "Ticket Code: " + ticket.getTicketCode() + "\n"
                    + (ticket.getSeatNumber() != null ? "Seat: " + ticket.getSeatNumber() + "\n" : "")
                    + "Booking Reference: " + bookingReference + "\n\n"
                    + "Please present your attached QR code at the venue entrance.\n\n"
                    + "See you there!\nEventSphere Team";

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
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>")
                .append("<body style='font-family: Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 20px;'>")
                .append("<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>")

                // Header Banner
                .append("<tr><td style='background: #7d4f50; padding: 24px; text-align: center; color: #ffffff;'>")
                .append("<h1 style='margin: 0; font-size: 22px;'>Order Receipt & Master Passes</h1>")
                .append("</td></tr>")

                // Content
                .append("<tr><td style='padding: 24px 28px 12px 28px;'>")
                .append("<p style='font-size: 15px; color: #333; margin: 0 0 10px 0;'>Hello <b>").append(escape(name)).append("</b>,</p>")
                .append("<p style='font-size: 14px; color: #555; margin: 0 0 16px 0;'>Thank you for your order! Your booking for <b>").append(escape(eventTitle)).append("</b> is confirmed.</p>")

                // Receipt Box
                .append("<div style='background: #fafafa; border: 1px solid #e2e8f0; border-radius: 6px; padding: 14px; margin-bottom: 20px; font-size: 13px; color: #444;'>")
                .append("<div style='display:flex; justify-content:space-between; margin-bottom: 6px;'><span>Booking Reference:</span> <b><code>").append(escape(bookingReference)).append("</code></b></div>")
                .append("<div style='display:flex; justify-content:space-between; margin-bottom: 6px;'><span>Total Tickets:</span> <b>").append(tickets.size()).append("</b></div>")
                .append("<div style='display:flex; justify-content:space-between;'><span>Total Paid:</span> <b style='color:#7d4f50;'>").append(escape(currency)).append(" ").append(formattedAmount).append("</b></div>")
                .append("</div>")
                .append("<h3 style='font-size: 16px; margin: 20px 0 10px 0; color: #333;'>Your Ticket Passes</h3>")
                .append("</td></tr>");

        // Tickets List
        for (int i = 0; i < tickets.size(); i++) {
            TicketEmailItem t = tickets.get(i);
            String cid = "receipt_qr" + i;
            html.append("<tr><td style='padding: 8px 28px;'>")
                    .append("<div style='border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; background-color: #ffffff;'>")
                    .append("<table width='100%' border='0' cellpadding='0' cellspacing='0'>")
                    .append("<tr><td valign='top' style='font-size: 14px; color: #333;'>")
                    .append("<p style='margin: 0 0 6px 0;'><b>Attendee:</b> ").append(escape(t.getAttendeeName())).append("</p>");
            if (t.getSeatNumber() != null && !t.getSeatNumber().isBlank()) {
                html.append("<p style='margin: 0 0 6px 0;'><b>Seat:</b> ").append(escape(t.getSeatNumber())).append("</p>");
            }
            html.append("<p style='margin: 0;'><b>Code:</b> <code style='font-size: 12px;'>").append(escape(t.getTicketCode())).append("</code></p>")
                    .append("</td></tr>")
                    .append("<tr><td align='center' style='padding-top: 14px;'>")
                    .append("<img src='cid:").append(cid).append("' width='160' height='160' alt='QR Code' style='display: block; margin: 0 auto; border: 1px solid #eee; border-radius: 4px; padding: 4px;' />")
                    .append("</td></tr></table>")
                    .append("</div></td></tr>");
        }

        // Footer
        html.append("<tr><td style='padding: 24px 28px; text-align: center; border-top: 1px solid #eee; font-size: 12px; color: #888;'>")
                .append("EventSphere • Automated Ticketing System</td></tr></table></body></html>");

        return html.toString();
    }

    private String buildIndividualTicketHtml(String name, String eventTitle, String bookingReference, TicketEmailItem ticket) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 20px;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 550px; background: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>"
                + "<tr><td style='background: #7d4f50; padding: 24px; text-align: center; color: #ffffff;'>"
                + "<h1 style='margin: 0; font-size: 20px;'>Your Admission Pass</h1>"
                + "</td></tr>"
                + "<tr><td style='padding: 24px 28px; text-align: center;'>"
                + "<p style='font-size: 15px; color: #333; margin: 0 0 8px 0;'>Hello <b>" + escape(name) + "</b>,</p>"
                + "<p style='font-size: 14px; color: #555; margin: 0 0 20px 0;'>Here is your official digital ticket for <b>" + escape(eventTitle) + "</b>.</p>"
                + "<div style='border: 2px dashed #7d4f50; border-radius: 8px; padding: 20px; background: #fafafa; display: inline-block; width: 85%;'>"
                + "<img src='cid:guest_qr' width='180' height='180' alt='Admission QR' style='display: block; margin: 0 auto; border: 1px solid #ddd; border-radius: 4px; padding: 4px; background: #fff;' />"
                + "<p style='font-size: 12px; color: #777; margin: 8px 0 12px 0;'>Scan at venue entrance</p>"
                + "<div style='font-size: 13px; color: #333; text-align: left; background: #fff; padding: 10px; border-radius: 6px; border: 1px solid #eee;'>"
                + "<div><b>Attendee:</b> " + escape(ticket.getAttendeeName()) + "</div>"
                + (ticket.getSeatNumber() != null ? "<div><b>Seat:</b> " + escape(ticket.getSeatNumber()) + "</div>" : "")
                + "<div><b>Ticket Code:</b> <code>" + escape(ticket.getTicketCode()) + "</code></div>"
                + "<div style='font-size: 11px; color: #888; margin-top: 4px;'>Booking Ref: " + escape(bookingReference) + "</div>"
                + "</div>"
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding: 20px 28px; text-align: center; border-top: 1px solid #eee; font-size: 12px; color: #888;'>"
                + "EventSphere • Please have this QR pass ready upon arrival.</td></tr></table></body></html>";
    }

    private void applyAntiSpamHeaders(MimeMessage message) throws Exception {
        message.setHeader("Precedence", "bulk");
        message.setHeader("Auto-Submitted", "auto-generated");
        message.setHeader("X-Mailer", "EventSphere Mailer 1.0");
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}