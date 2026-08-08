package lk.ijse.eventsphere.exception;

// Thrown when a PayHere webhook's MD5 signature fails re-verification, or
// the callback references an order/amount that doesn't match -> 400.
public class InvalidPaymentException extends RuntimeException {
    public InvalidPaymentException(String message) {
        super(message);
    }
}
