package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);


    Optional<Payment> findByMerchantOrderId(String merchantOrderId);
}
