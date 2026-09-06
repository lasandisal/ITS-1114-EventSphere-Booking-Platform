package lk.ijse.eventsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerApplicationRequestDTO {

    @NotBlank(message = "Business name is required")
    @Size(max = 150)
    private String businessName;

    @Size(max = 2000)
    private String bio;

    // KYC-lite: an identity claim an admin can cross-check manually, not a
    // verified/vendor-checked document. Deliberately a reference number, not
    // an uploaded ID photo or biometric scan — see design notes for why.
    @NotBlank(message = "National ID or passport number is required")
    @Size(max = 50)
    private String nicOrPassportNumber;

    @Size(max = 50)
    private String businessRegistrationNumber;

    @Size(max = 150)
    private String applicantName;

    @Size(max = 20)
    private String applicantPhone;
}
