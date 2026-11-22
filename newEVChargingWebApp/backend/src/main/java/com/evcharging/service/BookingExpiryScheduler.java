package com.evcharging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Auto-cancels (EXPIRES) bookings that were never started, so their slots return to the pool.
 */
@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingService bookingService;

    public BookingExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-check-ms}")
    public void expireOverdueBookings() {
        for (Integer bookingId : bookingService.findExpiredBookingIds()) {
            try {
                bookingService.expireBooking(bookingId); // own transaction per booking
            } catch (Exception e) {
                log.warn("Could not expire booking {}: {}", bookingId, e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-check-ms}")
    public void sendUpcomingBookingReminders() {
        for (Integer bookingId : bookingService.findBookingReminderIds()) {
            try {
                bookingService.sendBookingReminder(bookingId);
            } catch (Exception e) {
                log.warn("Could not send reminder for booking {}: {}", bookingId, e.getMessage());
            }
        }
    }


    @Scheduled(fixedDelayString = "${app.booking.expiry-check-ms}")
    public void expirePendingApprovalBookings() {

        for (Integer bookingId : bookingService.findExpiredApprovalBookingIds()) {
            try {
                bookingService.expirePendingApprovalBooking(bookingId);
            } catch (Exception e) {
                log.warn("Could not expire pending approval booking {}: {}", bookingId, e.getMessage());
            }
        }
    }


}
