package lk.ijse.eventsphere.service;

import java.util.List;

public interface EmailService {

    // Fire-and-forget from the caller's perspective — runs @Async so the
    // PayHere webhook handler returns promptly instead of blocking on SMTP.
    void sendBookingConfirmation(String recipientEmail, String recipientName,
                                 String eventTitle, String bookingReference,
                                 List<TicketEmailItem> tickets);
}
