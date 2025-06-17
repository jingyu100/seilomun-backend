package com.yju.team2.seilomun.domain.chat.controller;

import java.util.List;
import java.util.Map;

import com.yju.team2.seilomun.domain.chat.dto.ChatMessageDto;
import com.yju.team2.seilomun.domain.chat.dto.ChatRoomDto;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.chat.service.ChatService;
import com.yju.team2.seilomun.common.ApiResponseJson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    // WebSocket 메시지 핸들링
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessage,
                            //아래는 나중에 쓸려고 만든거임
                            SimpMessageHeaderAccessor headerAccessor) {
        log.debug("메시지 수신: {}", chatMessage);
        chatService.sendMessage(chatMessage);
    }

    // 이건 웹소켓 연결후에 클라이언트가 stomp 메세지를 보낼때 호출 되는거
    // 근데 프론트 만들어지면 getChats 이거랑 통합 할수도 있음 테스트 때매 만들어짐
    @MessageMapping("/chat.enterRoom")
    public void enterChatRoom(@Payload ChatMessageDto chatMessage) {
        chatService.userEnterRoom(
                chatMessage.getChatRoomId(),
                chatMessage.getSenderId(),
                chatMessage.getSenderType()
        );
    }

    @MessageMapping("/chat.leaveRoom")
    public void leaveChatRoom(@Payload ChatMessageDto chatMessage) {
        log.debug("채팅방 나가기: {}", chatMessage);
        chatService.userLeaveRoom(
                chatMessage.getChatRoomId(),
                chatMessage.getSenderId(),
                chatMessage.getSenderType()
        );
    }


    // 채팅방 생성 또는 조회
    @PostMapping("/chat/rooms")
    public ApiResponseJson createOrGetChatRoom(@RequestBody ChatRoomDto chatRoomDto,
                                               @AuthenticationPrincipal JwtUserDetails userDetails) {

        // 고객인 경우 고객 ID, 판매자인 경우 판매자 ID 확인
        Character userType;
        if (userDetails.getUserType().equals("CUSTOMER")) {
            userType = 'C';
        } else {
            userType = 'S';
        }

        Long userId = userDetails.getId();

        // 채팅방 생성 또는 조회
        ChatRoomDto chatRoom;
        if (userType == 'C') {
            chatRoom = chatService.createOrGetChatRoom(userId, chatRoomDto.getSellerId());
        } else {
            chatRoom = chatService.createOrGetChatRoom(chatRoomDto.getCustomerId(), userId);
        }

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "chatRoomId", chatRoom.getId(),
                "customerId", chatRoom.getCustomerId(),
                "customerNickname", chatRoom.getCustomerNickname(),
                "sellerId", chatRoom.getSellerId(),
                "sellerStoreName", chatRoom.getSellerStoreName(),
                "createdAt", chatRoom.getCreatedAt(),
                "lastMessage", chatRoom.getLastMessage(),
                "lastMessageTime", chatRoom.getLastMessageTime()
        ));
    }

    @GetMapping("/chat/rooms")
    public ApiResponseJson getChatRooms(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Character userType;
        if (userDetails.getUserType().equals("CUSTOMER")) {
            userType = 'C';
        } else {
            userType = 'S';
        }
        Long userId = userDetails.getId();
        List<ChatRoomDto> chatRooms = chatService.getChatRooms(userId, userType);
        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "chatRooms", chatRooms
        ));
    }


    @GetMapping("/chat/rooms/{id}")
    public ApiResponseJson getChats(@PathVariable Long id,
                                    @AuthenticationPrincipal JwtUserDetails userDetails) {
        Character userType;
        if (userDetails.getUserType().equals("CUSTOMER")) {
            userType = 'C';
        } else {
            userType = 'S';
        }
        chatService.userEnterRoom(id, userDetails.getId(), userType);

        List<ChatMessageDto> chatMessages = chatService.getChatMessages(id, userType);
        return new ApiResponseJson(HttpStatus.OK, Map.of("ok", chatMessages));
    }

}