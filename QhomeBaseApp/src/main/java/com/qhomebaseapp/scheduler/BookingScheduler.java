package com.qhomebaseapp.scheduler;

import com.qhomebaseapp.service.service.ServiceBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {

    private final ServiceBookingService serviceBookingService;

    /**
     * Runs every minute to check and cancel expired unpaid bookings (older than 10 minutes)
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void cancelExpiredBookings() {
        try {
            log.debug("Running scheduled task to cancel expired unpaid bookings");
            serviceBookingService.cancelExpiredUnpaidBookings();
        } catch (Exception e) {
            log.error("Error in scheduled task to cancel expired bookings", e);
        }
    }
}

