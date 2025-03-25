package com.yju.team2.seilomun.domain.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yju.team2.seilomun.domain.chat.entity.ChatMessage;
import com.yju.team2.seilomun.domain.chat.entity.ChatRoom;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
    List<ChatMessage> findByChatRoom_Id(Long id);
}