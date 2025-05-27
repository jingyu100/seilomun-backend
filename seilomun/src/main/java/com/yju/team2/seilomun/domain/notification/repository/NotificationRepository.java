package com.yju.team2.seilomun.domain.notification.repository;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 기존 메서드
    List<Notification> findAllByRecipientIdAndRecipientType(Long userId, Character userType);

    // 생성일 기준 내림차순으로 정렬된 알림 목록 조회
    List<Notification> findAllByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(Long userId, Character userType);

    // 읽지 않은 알림만 조회
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :userId AND n.recipientType = :userType AND n.isRead = 'N' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByRecipient(@Param("userId") Long userId, @Param("userType") Character userType);

    // 읽지 않은 알림 개수 조회
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :userId AND n.recipientType = :userType AND n.isRead = 'N'")
    int countUnreadNotificationsByRecipient(@Param("userId") Long userId, @Param("userType") Character userType);

    // 페이징을 위한 알림 조회
    @Query(value = "SELECT * FROM notifications WHERE recipient_id = :userId AND recipient_type = :userType ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Notification> findNotificationsByRecipientWithPaging(@Param("userId") Long userId,
                                                              @Param("userType") Character userType,
                                                              @Param("offset") int offset,
                                                              @Param("limit") int limit);

    // 특정 기간 이전의 알림 조회
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoffDate")
    List<Notification> findNotificationsOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    // 읽은 알림 중 오래된 것들 조회 
    @Query("SELECT n FROM Notification n WHERE n.isRead = 'Y' AND n.createdAt < :cutoffDate")
    List<Notification> findReadNotificationsOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}