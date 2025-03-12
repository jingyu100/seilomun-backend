package com.yju.team2.seilomun.domain.nofitication.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no_id")
    private Long id;

    @Column(name = "content", length = 255, nullable = false)
    private String content;

    //알림상태 보임 안보임
    @Column(name = "is_visible",nullable = false)
    private Character isVisible;

    @Column(name = "recipient_type", nullable = false)
    private Character recipientType;

    @Column(name = "recipient_id",nullable = false)
    private Long recipientId;

    @Column(name = "sender_type", nullable = false)
    private Character senderType;

    @Column(name = "sender_id",nullable = false)
    private Long senderId;

    @Column(name = "is_read", nullable = false)
    private Character isRead;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
