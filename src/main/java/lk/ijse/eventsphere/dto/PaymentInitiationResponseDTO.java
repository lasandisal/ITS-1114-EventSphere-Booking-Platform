package lk.ijse.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// Everything the frontend needs to hand off to PayHere's checkout
// (JS SDK or a hosted redirect form) — the hash proves to PayHere this
// request originated from our backend, not a tampered client-side amount.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiationResponseDTO {
    private String merchantId;
    private String orderId;
    private String amount;
    private String currency;
    private String hash;
    private String itemsDescription;
    private String returnUrl;
    private String cancelUrl;
    private String notifyUrl;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
}
