package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.enums.EventStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    /**
     * Unified search for published events with optional keyword and category filters.
     * Uses @EntityGraph to fetch category and venue in a single query (prevents N+1 and NPEs).
     */
    @EntityGraph(attributePaths = {"category", "venue"})
    @Query("""
        SELECT e FROM Event e 
        WHERE e.status = :status 
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) 
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
    """)
    Page<Event> searchPublishedEvents(
            @Param("status") EventStatus status,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

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
