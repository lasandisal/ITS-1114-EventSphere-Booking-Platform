package lk.ijse.eventsphere.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lk.ijse.eventsphere.service.EmailService;
import lk.ijse.eventsphere.service.TicketEmailItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendBookingConfirmation(String recipientEmail, String recipientName,
                                        String eventTitle, String bookingReference,
                                        List<TicketEmailItem> tickets) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart, required for inline images
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Your tickets for " + eventTitle + " — Booking " + bookingReference);
            helper.setText(buildHtmlBody(recipientName, eventTitle, bookingReference, tickets), true);

            for (int i = 0; i < tickets.size(); i++) {
                TicketEmailItem ticket = tickets.get(i);
                String cid = "qr" + i;
                helper.addInline(cid, new ByteArrayResource(ticket.getQrPng()), "image/png");
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            // Payment already succeeded and the booking is CONFIRMED — a
            // failed email must never roll that back. Log loudly so it can
            // be resent manually; the booking/ticket records remain the
            // source of truth regardless of email delivery.
            log.error("Failed to send booking confirmation email to {} for booking {}: {}",
                    recipientEmail, bookingReference, e.getMessage(), e);
        }
    }

    private String buildHtmlBody(String recipientName, String eventTitle, String bookingReference,
                                 List<TicketEmailItem> tickets) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>You're going to ").append(escape(eventTitle)).append("!</h2>")
                .append("<p>Hi ").append(escape(recipientName)).append(", your booking <b>")
                .append(escape(bookingReference)).append("</b> is confirmed.</p>");

        for (int i = 0; i < tickets.size(); i++) {
            TicketEmailItem t = tickets.get(i);
            String cid = "qr" + i;
            html.append("<div style='margin:16px 0;padding:12px;border:1px solid #ddd;'>")
                    .append("<p><b>Attendee:</b> ").append(escape(t.getAttendeeName())).append("</p>");
            if (t.getSeatNumber() != null && !t.getSeatNumber().isBlank()) {
                html.append("<p><b>Seat:</b> ").append(escape(t.getSeatNumber())).append("</p>");
            }
            html.append("<p><b>Ticket code:</b> ").append(escape(t.getTicketCode())).append("</p>")
                    .append("<img src='cid:").append(cid).append("' width='200' height='200' alt='QR ticket' />")
                    .append("</div>");
        }
        return html.toString();
    }

    // Minimal HTML-escaping — attendee-supplied strings (name, seat) land
    // directly in an HTML email body, so this isn't optional.
    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
