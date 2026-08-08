package lk.ijse.eventsphere.exception;

// Thrown inside the pessimistic-lock checkout transaction when requested
// quantity exceeds ticket_types.available_quantity -> 409 Conflict.
public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}
