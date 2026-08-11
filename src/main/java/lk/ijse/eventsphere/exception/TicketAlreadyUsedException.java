package lk.ijse.eventsphere.exception;

// The scanned ticket is valid and genuine, but entry was already granted
// earlier -> 409 Conflict, not 400 — the request itself isn't malformed,
// it's just too late.
public class TicketAlreadyUsedException extends RuntimeException {
    public TicketAlreadyUsedException(String message) {
        super(message);
    }
}
