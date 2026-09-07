package lk.ijse.eventsphere.config;

import lk.ijse.eventsphere.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Runs every minute, releasing inventory for any PENDING booking whose
// checkout hold (booking.hold-ttl-minutes) has lapsed. Without this, a user
// who abandons checkout locks that inventory forever.
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(initialDelay = 5_000, fixedRate = 60_000)
    public void releaseExpiredHolds() {
        log.debug("Running scheduled task: releaseExpiredHolds()");
        bookingService.expireStaleBookings();
    }
}

