package lk.ijse.eventsphere.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// Plain data carrier for EmailService — deliberately NOT the Ticket entity.
// The email send runs @Async, potentially after the originating transaction
// (and Hibernate session) has already closed, so passing the entity across
// that boundary risks a LazyInitializationException. Everything needed is
// extracted to plain fields before the async call.
@Getter
@Builder
@AllArgsConstructor
public class TicketEmailItem {
    private final String attendeeName;
    private final String seatNumber;
    private final String ticketCode;
    private final byte[] qrPng;
    private final String ticketTypeName;
    private final String eventDate;
    private final String eventTime;
    private final String venueName;
    private final String venueAddress;
}
