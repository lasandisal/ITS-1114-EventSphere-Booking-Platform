package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.CheckIn;
import lk.ijse.eventsphere.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    Optional<CheckIn> findFirstByTicketOrderByCheckInTimeAsc(Ticket ticket);
}
