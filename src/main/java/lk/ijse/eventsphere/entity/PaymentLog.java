package lk.ijse.eventsphere.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Every incoming IPG webhook is logged here regardless of outcome — including
// ones that don't match a known payment — for audit and idempotency. `processed`
// is flipped true only after the booking state transition completes, so a
// duplicate webhook delivery can check this before acting a second time.
@Entity
@Table(name = "payment_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Lob
    @Column(name = "raw_payload", nullable = false, columnDefinition = "JSON")
    private String rawPayload;

    @Column(name = "status_code", length = 10)
    private String statusCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }
}
