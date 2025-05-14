package com.yju.team2.seilomun.domain.notification.repository;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
