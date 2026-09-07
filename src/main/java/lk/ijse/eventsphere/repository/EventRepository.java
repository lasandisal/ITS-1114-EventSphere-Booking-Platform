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

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Eagerly fetch associations for organizer dashboard
    @EntityGraph(attributePaths = {"organizer", "category", "venue"})
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    // Eagerly fetch associations for admin dashboard
    @Override
    @EntityGraph(attributePaths = {"organizer", "category", "venue"})
    Page<Event> findAll(Pageable pageable);

    // Eagerly fetch associations for single event lookup
    @Override
    @EntityGraph(attributePaths = {"organizer", "category", "venue"})
    Optional<Event> findById(Long id);

    // Eagerly fetch associations for public search/browse
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

    long countByStatus(EventStatus status);

    @Query("""
        SELECT COUNT(e) > 0 FROM Event e
        WHERE e.organizer.id = :organizerId
          AND LOWER(TRIM(e.title)) = LOWER(TRIM(:title))
          AND e.startDatetime >= :startOfDay AND e.startDatetime <= :endOfDay
          AND e.status != lk.ijse.eventsphere.enums.EventStatus.CANCELLED
          AND (:excludeEventId IS NULL OR e.id != :excludeEventId)
    """)
    boolean existsDuplicateForOrganizer(
            @Param("organizerId") Long organizerId,
            @Param("title") String title,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("excludeEventId") Long excludeEventId
    );

    @Query("""
        SELECT COUNT(e) > 0 FROM Event e
        WHERE e.venue.id = :venueId
          AND e.status != lk.ijse.eventsphere.enums.EventStatus.CANCELLED
          AND (:newStart < e.endDatetime AND :newEnd > e.startDatetime)
          AND (:excludeEventId IS NULL OR e.id != :excludeEventId)
    """)
    boolean existsVenueCollision(
            @Param("venueId") Long venueId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludeEventId") Long excludeEventId
    );
}