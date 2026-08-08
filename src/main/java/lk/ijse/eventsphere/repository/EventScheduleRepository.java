package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {

    List<EventSchedule> findByEventIdOrderByDisplayOrderAsc(Long eventId);
}
