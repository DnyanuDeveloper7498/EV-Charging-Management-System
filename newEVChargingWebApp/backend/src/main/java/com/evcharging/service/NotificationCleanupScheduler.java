package com.evcharging.service;

import com.evcharging.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Deletes old notifications so the table does not grow forever. Runs every night at 03:00.
 */
@Component
public class NotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupScheduler.class);

    private final NotificationRepository notificationRepository;
    private final Clock clock;
    private final long readRetentionDays;
    private final long maxRetentionDays;

    public NotificationCleanupScheduler(NotificationRepository notificationRepository, Clock clock, @Value("${app.notification.read-retention-days:30}") long readRetentionDays, @Value("${app.notification.max-retention-days:90}") long maxRetentionDays) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.readRetentionDays = readRetentionDays;
        this.maxRetentionDays = maxRetentionDays;
    }

    @Scheduled(cron = "0 0 3  * * *", zone = "${app.timezone}")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);

        int deleted = notificationRepository.deleteOldNotifications(now.minusDays(readRetentionDays), now.minusDays(maxRetentionDays));

        if (deleted > 0) {
            log.info("Deleted {} old notifications", deleted);
        }
    }
}