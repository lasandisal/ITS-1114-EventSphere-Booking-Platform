package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Organizer;
import lk.ijse.eventsphere.enums.OrganizerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    Optional<Organizer> findByUserId(Long userId);

//    List<Organizer> findByVerifiedFalse();

    List<Organizer> findByStatus(OrganizerStatus status);
}
