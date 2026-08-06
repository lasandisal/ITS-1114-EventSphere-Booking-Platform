package lk.ijse.eventsphere.enums;

public enum TicketStatus {
    VALID,        // issued, not yet used
    CHECKED_IN,   // scanned at venue entry
    CANCELLED     // ticket cancelled individually (part of a larger booking)
}
