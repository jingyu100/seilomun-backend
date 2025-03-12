package com.yju.team2.seilomun.domain.chat.repository;

import com.yju.team2.seilomun.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
