package lk.ijse.eventsphere.exception;

// e.g. event/ticket type/booking looked up by id that doesn't exist -> 404.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
