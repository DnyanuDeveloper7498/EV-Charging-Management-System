package com.evcharging.controller;


import com.evcharging.model.Notification;
import com.evcharging.service.InAppNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final InAppNotificationService notificationService;

    public NotificationController(InAppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Get all notifications

    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications() {
        return ResponseEntity.ok(notificationService.getUserNotifications());
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        return ResponseEntity.ok(notificationService.getUnreadNotifications());
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {

        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount()));
    }


    // Mark single notification as read
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long notificationId) {

        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }


    // Mark all notifications as read
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {

        notificationService.markAllAsRead();

        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }


    // Delete a single notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long notificationId) {

        notificationService.deleteNotification(notificationId);

        return ResponseEntity.ok(Map.of("message", "Notification deleted."));
    }

    // Delete all notifications of the logged-in user
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAllNotifications() {

        notificationService.deleteAllNotifications();

        return ResponseEntity.ok(Map.of("message", "All notifications deleted."));
    }


}
