package lk.ijse.eventsphere.entity;

import jakarta.persistence.*;
import lk.ijse.eventsphere.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1-to-1: a booking gets one authoritative payment record; a retry after
    // failure is a new booking attempt, not a second payment row on this one.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String provider = "PAYHERE";

    @Column(name = "merchant_order_id", nullable = false, unique = true, length = 80)
    private String merchantOrderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "md5_signature", length = 255)
    private String md5Signature;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
