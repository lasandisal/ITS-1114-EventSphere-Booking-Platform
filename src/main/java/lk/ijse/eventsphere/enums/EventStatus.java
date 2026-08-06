package lk.ijse.eventsphere.enums;

public enum EventStatus {
    DRAFT,       // being created/edited by organizer, not visible to attendees
    PUBLISHED,   // live, searchable, bookable
    CANCELLED,   // organizer cancelled the event
    COMPLETED    // event date has passed
}
