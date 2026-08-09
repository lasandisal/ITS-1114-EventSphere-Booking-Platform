package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.PaymentInitiationResponseDTO;

import java.util.Map;

public interface PaymentService {

    // Phase 1: locks in a Payment record against the (still-PENDING) booking
    // and returns everything the frontend hands to PayHere's checkout.
    PaymentInitiationResponseDTO initiatePayment(Long bookingId);

    // Phase 2: PayHere's server-to-server webhook callback. params is the
    // raw form-encoded POST body as key/value pairs.
    void handleNotify(Map<String, String> params);
}
