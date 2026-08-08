package lk.ijse.eventsphere.constant;

// Centralized user-facing message strings — keeps wording consistent across
// controllers and makes copy changes a one-line edit instead of a grep.
public final class ResponseMessage {

    private ResponseMessage() {}

    public static final String REGISTRATION_SUCCESS = "Account created successfully";
    public static final String LOGIN_SUCCESS = "Login successful";
    public static final String EMAIL_ALREADY_REGISTERED = "An account with this email already exists";
}
