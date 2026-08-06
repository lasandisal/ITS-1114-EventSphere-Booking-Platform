package lk.ijse.eventsphere.entity;

import jakarta.persistence.*;
import lk.ijse.eventsphere.enums.TicketStatus;
import lombok.*;

/**
 * No soft-delete flag here either — TicketStatus.CANCELLED already covers an individual
 * ticket being voided within a booking, and the row must persist as an attendee/check-in
 * record regardless.
 */
@Entity
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(columnNames = "ticketCode"))
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
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(nullable = false, length = 100)
    private String attendeeName;

    @Column(nullable = false, length = 150)
    private String attendeeEmail;

    private String seatNumber; // nullable — not every event is seated

    @Column(nullable = false, unique = true, length = 64)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.VALID;
}
