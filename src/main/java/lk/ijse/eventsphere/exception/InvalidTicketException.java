package lk.ijse.eventsphere.exception;

// Signature verification failed, ticket doesn't exist, is cancelled, or its
// booking was never confirmed -> 400. Distinct from TicketAlreadyUsedException
// because "already used" is a different, more specific situation for the
// gate staff to see (they'll want to know it was already scanned, not that
// something is generically wrong with the code).
public class InvalidTicketException extends RuntimeException {
    public InvalidTicketException(String message) {
        super(message);
    }
}
