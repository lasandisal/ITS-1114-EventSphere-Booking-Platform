package lk.ijse.eventsphere.exception;

// e.g. registering with an email that's already taken -> 409 Conflict.
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
