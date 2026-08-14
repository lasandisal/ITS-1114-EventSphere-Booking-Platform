package lk.ijse.eventsphere.service;

import java.math.BigDecimal;
import java.util.List;

public interface EmailService {

    // Legacy method (optional to keep for compatibility)
    void sendBookingConfirmation(String recipientEmail, String recipientName,
                                 String eventTitle, String bookingReference,
                                 List<TicketEmailItem> tickets);

    /**
     * Master Receipt & Full Passes sent to the primary booker (financial purchaser).
     */
    void sendOrderReceipt(String recipientEmail, String recipientName,
                          String eventTitle, String bookingReference,
                          BigDecimal totalAmount, String currency,
                          List<TicketEmailItem> tickets);

    /**
     * Individual admission pass sent to a specific guest/attendee.
     */
    void sendIndividualTicketPass(String attendeeEmail, String attendeeName,
                                  String eventTitle, String bookingReference,
                                  TicketEmailItem ticket);
}