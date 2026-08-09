package lk.ijse.eventsphere.config;

import lk.ijse.eventsphere.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Runs every minute, releasing inventory for any PENDING booking whose
// checkout hold (booking.hold-ttl-minutes) has lapsed. Without this, a user
// who abandons checkout locks that inventory forever.
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedRate = 60_000)
    public void releaseExpiredHolds() {
        bookingService.expireStaleBookings();
    }
}
