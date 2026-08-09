package lk.ijse.eventsphere.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Lob
    private String bio;

    // KYC-lite fields — see OrganizerApplicationRequestDTO for why these are
    // reference numbers, not uploaded documents or biometric data. Plaintext
    // storage here is a known limitation worth naming in the report; a
    // production system would encrypt these at rest or hand off to a real
    // KYC vendor instead of storing them directly.
    @Column(name = "nic_or_passport_number", length = 50)
    private String nicOrPassportNumber;

    @Column(name = "business_registration_number", length = 50)
    private String businessRegistrationNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
