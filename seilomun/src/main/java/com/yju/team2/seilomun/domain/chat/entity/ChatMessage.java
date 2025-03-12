package com.yju.team2.seilomun.domain.chat.entity;

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
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cm_id")
    private Long id;

    @Column(name = "sender_type", nullable = false)
    private Character senderType;

    @Column(name = "sender_id" , nullable = false)
    private Long senderId;

    @Column(name = "content",nullable = false, length = 255)
    private String content;

    @Column(name = "created_at",nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "is_read",nullable = false , length = 1)
    private Character isRead;

    @ManyToOne
    @JoinColumn(name = "cr_id")
    private ChatRoom chatRoom;
}
