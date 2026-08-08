package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    // Backs AI assistant's search_events(keyword, category) tool and the
    // public discovery endpoint — only ever searches PUBLISHED events.
    Page<Event> findByStatusAndTitleContainingIgnoreCase(
            EventStatus status, String keyword, Pageable pageable);

    Page<Event> findByStatusAndCategory_Id(
            EventStatus status, Long categoryId, Pageable pageable);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    // Delete guards — a category/venue with events still pointing at it
    // can't be removed without breaking those FKs; the service layer checks
    // these before calling repository.delete().
    boolean existsByCategoryId(Long categoryId);

    boolean existsByVenueId(Long venueId);
}
