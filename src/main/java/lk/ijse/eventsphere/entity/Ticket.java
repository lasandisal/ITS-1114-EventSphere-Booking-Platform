package lk.ijse.eventsphere.entity;

import jakarta.persistence.*;
import lk.ijse.eventsphere.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ticket_code is a UUID; the QR payload encodes ticket_code + HMAC-SHA256(ticket_code, secret)
// so gate scanners can reject a forged code before hitting the DB. DB status is still the
// single source of truth for whether entry was already granted.
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_item_id", nullable = false)
    private BookingItem bookingItem;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 64)
    private String ticketCode;

    @Column(name = "attendee_name", nullable = false, length = 150)
    private String attendeeName;

    @Column(name = "attendee_email", nullable = false, length = 150)
    private String attendeeEmail;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.VALID;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
}
