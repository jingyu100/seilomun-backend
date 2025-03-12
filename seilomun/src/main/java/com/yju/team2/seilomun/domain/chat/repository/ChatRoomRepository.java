package com.yju.team2.seilomun.domain.chat.repository;

import com.yju.team2.seilomun.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
