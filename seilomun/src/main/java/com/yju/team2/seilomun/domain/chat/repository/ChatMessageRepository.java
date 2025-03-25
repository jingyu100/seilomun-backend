package com.yju.team2.seilomun.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yju.team2.seilomun.domain.chat.entity.ChatMessage;
import com.yju.team2.seilomun.domain.chat.entity.ChatRoom;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 가장 마지막으로 온 메세지 찾는거
    Optional<ChatMessage> findTopByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
    List<ChatMessage> findByChatRoomOrderByCreatedAt(ChatRoom chatRoom);
    // 안읽은 메세지 수 찾는거
    int countByChatRoomAndIsReadAndSenderType(ChatRoom chatRoom, Character isRead, Character senderType);
}