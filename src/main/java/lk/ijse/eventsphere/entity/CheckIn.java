package lk.ijse.eventsphere.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Kept as an append-only log rather than a single flag on Ticket — every scan
// attempt is recorded (including attempts on an already-USED ticket). The
// ticket's own status is the source of truth for whether entry was granted.
@Entity
@Table(name = "check_ins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_in_by", nullable = false)
    private User checkedInBy;

    @Column(name = "check_in_time", nullable = false, updatable = false)
    private LocalDateTime checkInTime;

    @Column(name = "location_note", length = 100)
    private String locationNote;

    @PrePersist
    protected void onCreate() {
        this.checkInTime = LocalDateTime.now();
    }
}
