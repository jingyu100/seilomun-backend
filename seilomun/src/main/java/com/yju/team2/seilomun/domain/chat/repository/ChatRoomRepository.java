package com.yju.team2.seilomun.domain.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yju.team2.seilomun.domain.chat.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.customer.id = :customerId AND cr.seller.id = :sellerId")
    Optional<ChatRoom> findByCustomerIdAndSellerId(@Param("customerId") Long customerId, @Param("sellerId") Long sellerId);
}