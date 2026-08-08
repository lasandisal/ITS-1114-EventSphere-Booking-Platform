package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    List<CheckIn> findByTicketId(Long ticketId);
}
