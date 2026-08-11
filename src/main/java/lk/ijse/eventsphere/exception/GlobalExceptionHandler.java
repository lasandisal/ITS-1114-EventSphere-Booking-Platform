package lk.ijse.eventsphere.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// Catches exceptions across every controller and returns the same
// ErrorResponseDTO shape everywhere, per the coursework's "Global Controller
// Advice" requirement — separates operational errors (expected, domain-level)
// from systemic bugs (unexpected, logged as 500).
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponseDTO> handleInsufficientInventory(
            InsufficientInventoryException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPayment(
            InvalidPaymentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidTicketException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTicket(
            InvalidTicketException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(TicketAlreadyUsedException.class)
    public ResponseEntity<ErrorResponseDTO> handleTicketAlreadyUsed(
            TicketAlreadyUsedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request, null);
    }

    // Business-rule violations raised as plain Java exceptions (bad date
    // ranges, delete blocked by referential dependents, etc.) — these are
    // expected/operational, not bugs, so they get 400 with the real message
    // instead of falling through to the generic 500 handler below.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(
            RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // Catch-all — anything not explicitly mapped above is treated as an
    // unexpected systemic bug, not surfaced to the client in detail.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponseDTO> build(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> validationErrors) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
