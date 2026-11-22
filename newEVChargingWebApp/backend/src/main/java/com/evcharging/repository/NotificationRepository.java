package com.evcharging.repository;

import com.evcharging.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Integer userId);


    long countByUserIdAndIsReadFalse(Integer userId);

    Optional<Notification> findByNotificationIdAndUserId(Long notificationId, Integer userId);

    @Modifying
    @Query("delete from Notification n where (n.isRead = true and n.createdAt < :readCutoff) or n.createdAt < :anyCutoff")
    int deleteOldNotifications(@Param("readCutoff") LocalDateTime readCutoff, @Param("anyCutoff") LocalDateTime anyCutoff);


    @Modifying
    @Query("delete from Notification n where n.notificationId = :notificationId and n.userId = :userId")
    int deleteByIdAndUser(@Param("notificationId") Long notificationId, @Param("userId") Integer userId);

    @Modifying
    @Query("delete from Notification n where n.userId = :userId")
    int deleteAllByUser(@Param("userId") Integer userId);

    boolean existsByUserId(Integer userId);




}
