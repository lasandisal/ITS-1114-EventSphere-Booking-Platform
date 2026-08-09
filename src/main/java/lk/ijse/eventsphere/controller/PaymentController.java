package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.PaymentInitiationResponseDTO;
import lk.ijse.eventsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/initiate/{bookingId}")
    public ResponseEntity<CommonResponse<PaymentInitiationResponseDTO>> initiate(@PathVariable Long bookingId) {
        PaymentInitiationResponseDTO response = paymentService.initiatePayment(bookingId);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Checkout ready", response));
    }

    // PayHere calls this server-to-server with NO user JWT — see
    // SecurityConfig, this path is permitAll() and authenticated instead by
    // MD5 signature re-verification inside PaymentService.handleNotify.
    // PayHere posts application/x-www-form-urlencoded, not JSON, hence the
    // raw param map rather than a @RequestBody DTO.
    @PostMapping(value = "/notify", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> notify(@RequestParam Map<String, String> params) {
        paymentService.handleNotify(params);
        // Always 200 — a bad/forged signature is logged and permanently
        // ignored (see PaymentServiceImpl), not something PayHere should
        // retry, so there's no reason to return an error status for it.
        return ResponseEntity.ok("OK");
    }
}
