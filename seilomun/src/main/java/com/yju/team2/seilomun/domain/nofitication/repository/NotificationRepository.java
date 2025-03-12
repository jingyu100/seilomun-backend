package com.yju.team2.seilomun.domain.nofitication.repository;

import com.yju.team2.seilomun.domain.nofitication.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
