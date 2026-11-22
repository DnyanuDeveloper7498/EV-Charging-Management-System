package com.evcharging.service;

import com.evcharging.model.Notification;
import com.evcharging.repository.NotificationRepository;
import com.evcharging.security.SecurityUtils;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public InAppNotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {

        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Create and send notification
    @Transactional
    public Notification createNotification(Integer userId, Integer bookingId, String title, String message, String notificationType) {

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setBookingId(bookingId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setIsRead(false);

        Notification savedNotification = notificationRepository.save(notification);

        // Send through WebSocket only after the DB transaction has committed
//        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", savedNotification);

        sendAfterCommit(userId, savedNotification);

        return savedNotification;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotificationIsolated(Integer userId, Integer bookingId, String title, String message, String notificationType) {

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setBookingId(bookingId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setIsRead(false);

        Notification savedNotification = notificationRepository.save(notification);

        sendAfterCommit(userId, savedNotification);

        return savedNotification;
    }


    // Get all notifications
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications() {

        Integer userId = SecurityUtils.currentUserId();

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get unread notifications
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications() {

        Integer userId = SecurityUtils.currentUserId();

        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    // Get unread notification count
    @Transactional(readOnly = true)
    public long getUnreadCount() {

        Integer userId = SecurityUtils.currentUserId();

        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // Mark single notification as read
    @Transactional
    public Notification markAsRead(Long notificationId) {

        Integer userId = SecurityUtils.currentUserId();

        Notification notification = notificationRepository.findByNotificationIdAndUserId(notificationId, userId).orElseThrow(() -> new IllegalArgumentException("Notification not found."));

        notification.setIsRead(true);

        return notificationRepository.save(notification);
    }

    // Mark all notifications as read
    @Transactional
    public void markAllAsRead() {

        Integer userId = SecurityUtils.currentUserId();

        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        notifications.forEach(notification -> notification.setIsRead(true));

        notificationRepository.saveAll(notifications);
    }


    @Transactional
    public void deleteNotification(Long notificationId) {

        Integer userId = SecurityUtils.currentUserId();

        int deleted = notificationRepository.deleteByIdAndUser(notificationId, userId);

        if (deleted == 0) {
            throw new IllegalArgumentException("Notification not found.");
        }
    }


    @Transactional
    public void deleteAllNotifications() {

        notificationRepository.deleteAllByUser(SecurityUtils.currentUserId());
    }


    public void sendAfterCommit(Integer userId, Notification notification) {
        Runnable send = () -> messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", notification);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }


}
