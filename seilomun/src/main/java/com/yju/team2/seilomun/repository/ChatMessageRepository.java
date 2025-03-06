package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
