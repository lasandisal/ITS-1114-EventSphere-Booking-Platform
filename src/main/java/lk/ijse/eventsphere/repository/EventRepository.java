package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // backs the AI assistant's search_events(keyword, category) tool and the attendee
    // discovery/search screen. Only ever searches PUBLISHED events — drafts and
    // cancelled events are never attendee-visible.
    @Query("""
            SELECT e FROM Event e
            WHERE e.status = lk.ijse.eventsphere.enums.EventStatus.PUBLISHED
              AND (:keyword IS NULL OR
                   LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category IS NULL OR LOWER(e.category) = LOWER(:category))
            ORDER BY e.startDateTime ASC
            """)
    Page<Event> search(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);

    // backs the AI assistant's get_event_details(eventId) tool and the event detail page —
    // attendees may only fetch full details for a published event
    Optional<Event> findByIdAndStatus(Long id, EventStatus status);

    // organizer's own "my events" management screen — includes DRAFT/CANCELLED, not just PUBLISHED
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);
}