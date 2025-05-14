package com.yju.team2.seilomun.domain.notification.repository;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 알림 리스트 출력
    List<Notification> findAllByRecipientIdAndRecipientType(Long userId, Character userType);

}
