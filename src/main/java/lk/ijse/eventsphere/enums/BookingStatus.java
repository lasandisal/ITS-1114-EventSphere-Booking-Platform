package lk.ijse.eventsphere.enums;

public enum BookingStatus {
    PENDING,     // inventory held, awaiting confirmation (e.g. 10-min hold for future payment step)
    CONFIRMED,   // booking finalized, tickets valid
    CANCELLED,   // cancelled by user or organizer, inventory released
    EXPIRED      // PENDING hold timed out without confirmation, inventory released
}

