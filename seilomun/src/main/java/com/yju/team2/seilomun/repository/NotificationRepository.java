package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
