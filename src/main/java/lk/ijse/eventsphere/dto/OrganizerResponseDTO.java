package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerResponseDTO {
    private Long id;
    private String businessName;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String nicOrPassportNumber;
    private String businessRegistrationNumber;
    private String bio;
    private String status;
    private boolean verified;
    private LocalDateTime createdAt;
}