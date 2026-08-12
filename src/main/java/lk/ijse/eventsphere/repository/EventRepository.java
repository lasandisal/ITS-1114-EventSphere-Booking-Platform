package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Event;
import lk.ijse.eventsphere.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    // Eagerly fetch organizer, category, and venue for individual event lookups
    @EntityGraph(attributePaths = {"organizer", "category", "venue"})
    Optional<Event> findById(Long id);

    // Eagerly fetch associations for search/list queries
    @EntityGraph(attributePaths = {"organizer", "category", "venue"})
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

    boolean existsByCategoryId(Long categoryId);
    boolean existsByVenueId(Long venueId);
}