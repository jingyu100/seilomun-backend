package com.yju.team2.seilomun.domain.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.yju.team2.seilomun.domain.chat.dto.ChatMessageDto;
import com.yju.team2.seilomun.domain.chat.dto.ChatRoomDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.chat.entity.ChatMessage;
import com.yju.team2.seilomun.domain.chat.entity.ChatRoom;
import com.yju.team2.seilomun.domain.chat.repository.ChatMessageRepository;
import com.yju.team2.seilomun.domain.chat.repository.ChatRoomRepository;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;
    private final ObjectMapper objectMapper;

    // 10분
    private static final long EXPIRE_TIME = 600;
    // 채팅방 생성 또는 기존 채팅방 찾기
    @Transactional
    public ChatRoomDto createOrGetChatRoom(Long customerId, Long sellerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        
        ChatRoom chatRoom;

        // 이미 존재하는 채팅방 확인
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findByCustomerIdAndSellerId(customerId, sellerId);
        
        // 없으면 새 채팅방
        if (optionalChatRoom.isEmpty()) {
            chatRoom = ChatRoom.builder()
                    .customer(customer)
                    .seller(seller)
                    .creationAt(LocalDateTime.now())
                    .build();

            chatRoomRepository.save(chatRoom);
        } else {
            chatRoom = optionalChatRoom.get();
        }

        // 마지막 메시지 정보 구하기
        String lastMessage = "";
        LocalDateTime lastMessageTime = chatRoom.getCreationAt();

        Optional<ChatMessage> lastMessageOptional = chatMessageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom);
        if (lastMessageOptional.isPresent()) {
            ChatMessage lastChatMessage = lastMessageOptional.get();
            lastMessage = lastChatMessage.getContent();
            lastMessageTime = lastChatMessage.getCreatedAt();
        }

        // DTO 생성 및 반환
        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .customerId(customerId)
                .customerNickname(customer.getNickname())
                .sellerId(sellerId)
                .sellerStoreName(seller.getStoreName())
                .createdAt(chatRoom.getCreationAt())
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .customerPhotoUrl(customer.getProfileImageUrl())
                .sellerPhotoUrl(seller.getSellerPhotos().getFirst().toString())
                .build();
    }

    // 메시지 전송
    @Transactional
    public ChatMessageDto sendMessage(ChatMessageDto chatMessageDto) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        // 수신자 타입
        Character receiverType;
            if (chatMessageDto.getSenderType().equals('C')) {
                receiverType = 'S';
            } else {
                receiverType = 'C';
            }

        // 수신자가 채팅방에 있는지 확인
        boolean isReceiverInRoom = isUserInChatRoom(chatMessageDto.getChatRoomId(),
                                                    chatMessageDto.getReceiverId(),
                                                    receiverType);
        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderType(chatMessageDto.getSenderType())
                .senderId(chatMessageDto.getSenderId())
                .content(chatMessageDto.getContent())
                .createdAt(LocalDateTime.now())
                .isRead(isReceiverInRoom ? 'Y' : 'N')
                .build();

        chatMessageRepository.save(chatMessage);

        // 타임스탬프 설정
        chatMessageDto.setTimestamp(chatMessage.getCreatedAt());
        chatMessageDto.setRead(chatMessage.getIsRead());
        chatMessageDto.setId(chatMessage.getId());

        // Redis를 통해 메시지 발행
        try {
            // JSON 문자열로 직렬화
            String jsonMessage = objectMapper.writeValueAsString(chatMessageDto);

            // Redis에 메시지 발행
            redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);

            log.debug("메시지 발행 완료: {}", jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("메시지 발행 중 에러 발생: {}", e.getMessage(), e);
        }
        return chatMessageDto;
    }

    // 채팅 방 찾기
    @Transactional
    public List<ChatRoomDto> getChatRooms(Long id, Character userType) {
        List<ChatRoom> chatRoomList;
        if (userType.equals('C')) {
            chatRoomList = chatRoomRepository.findByCustomer_Id(id);
        } else {
            chatRoomList = chatRoomRepository.findBySeller_Id(id);
        }
        List<ChatRoomDto> chatRoomDtoList = new ArrayList<>();

        for (ChatRoom chatRoom : chatRoomList) {
            Optional<ChatMessage> lastMessageOptional = chatMessageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom);
            //안읽은 메세지 수 찾는거
            int count;
            if (userType.equals('C')) {
                // 상대방이 보낸거 갯수찾기
                count = chatMessageRepository.countByChatRoomAndIsReadAndSenderType(chatRoom, 'N', 'S');
            }
            else {
                count = chatMessageRepository.countByChatRoomAndIsReadAndSenderType(chatRoom, 'N', 'C');
            }

            // 메세지 내용 담을 변수 없으면 공백
            String lastMessage = "";
            LocalDateTime lastMessageTime = chatRoom.getCreationAt();

            // 채팅방에 마지막 메세지가 있다면 마지막 메세지 정보 가져오기
            if (lastMessageOptional.isPresent()) {
                ChatMessage lastChatMessage = lastMessageOptional.get();
                lastMessage = lastChatMessage.getContent();
                lastMessageTime = lastChatMessage.getCreatedAt();
            }

            ChatRoomDto chatRoomDto = ChatRoomDto.builder()
                    .id(chatRoom.getId())
                    .customerId(chatRoom.getCustomer().getId())
                    .customerNickname(chatRoom.getCustomer().getNickname())
                    .sellerId(chatRoom.getSeller().getId())
                    .sellerStoreName(chatRoom.getSeller().getStoreName())
                    .createdAt(chatRoom.getCreationAt())
                    .lastMessage(lastMessage)
                    .lastMessageTime(lastMessageTime)
                    .unreadCount(count)
                    .customerPhotoUrl(chatRoom.getCustomer().getProfileImageUrl())
                    .sellerPhotoUrl(chatRoom.getSeller().getSellerPhotos().getFirst().toString())
                    .build();

            chatRoomDtoList.add(chatRoomDto);
        }
        // 마지막 메시지 시간 기준으로 정렬 (최신 메시지가 위로)
        chatRoomDtoList.sort((room1, room2) ->
                room2.getLastMessageTime().compareTo(room1.getLastMessageTime()));
        return chatRoomDtoList;
    }

    //해당 채팅방 채팅 가져오기
    @Transactional
    public List<ChatMessageDto> getChatMessages(Long chatRoomId, Character userType) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        Customer customer = chatRoom.getCustomer();
        Seller seller = chatRoom.getSeller();
        // 생성 시간 기준으로 정렬
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedAt(chatRoom);
        // 읽음 상태 업데이트가 필요한 메시지들 수집
        List<ChatMessage> messagesToUpdate = new ArrayList<>();
        // 반환할 dto
        List<ChatMessageDto> chatMessageDtoList = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessages) {
            // 상대방이 보낸 메세지이면서 읽지 않았을때
            if (chatMessage.getSenderType() != userType && chatMessage.getIsRead().equals('N')) {
                chatMessage.updateIsRead('Y');
                messagesToUpdate.add(chatMessage);
            }
        }
        // 업데이트 필요한것들이 있다면 일괄 업데이트
        if (!messagesToUpdate.isEmpty()) {
            chatMessageRepository.saveAll(messagesToUpdate);
        }

        for (ChatMessage chatMessage : chatMessages) {
            String senderName;
            Long senderId;
            Long receiverId;

            if (chatMessage.getSenderType().equals('C')) {
                senderName = customer.getNickname();
                senderId = customer.getId();
                receiverId = seller.getId();
            } else {
                senderName = seller.getStoreName();
                senderId = seller.getId();
                receiverId = customer.getId();
            }

            ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                    .type(ChatMessageDto.MessageType.CHAT)
                    .chatRoomId(chatRoomId)
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .senderName(senderName)
                    .content(chatMessage.getContent())
                    .senderType(chatMessage.getSenderType())
                    .timestamp(chatMessage.getCreatedAt())
                    .read(chatMessage.getIsRead())
                    .build();
            chatMessageDtoList.add(chatMessageDto);
        }
        return chatMessageDtoList;
    }

    // 사용자가 채팅방에 있는지 확인
    public boolean isUserInChatRoom(Long chatRoomId, Long userId, Character userType) {
        String key = "chat_active:" + chatRoomId + ":" + userId + ":" + userType;
        Boolean exists = redisTemplate.hasKey(key);
        log.debug("사용자 {}(타입:{}) 채팅방 {} 존재 여부: {}", userId, userType, chatRoomId, exists);
        return Boolean.TRUE.equals(exists);
    }

    // 사용자 채팅방 입장 처리
    @Transactional
    public void userEnterRoom(Long chatRoomId, Long userId, Character userType) {
        // redis에 사용자 활성 상태 저장
        String key = "chat_active:" + chatRoomId + ":" + userId + ":" + userType;
        redisTemplate.opsForValue().set(key, "active", EXPIRE_TIME, TimeUnit.SECONDS);
        // 입장 알림
        sendUserEnterNotification(chatRoomId, userId, userType);
    }


    // 사용자 채팅방 나가기 처리
    @Transactional
    public void userLeaveRoom(Long chatRoomId, Long userId, Character userType) {
        // Redis에서 사용자 활성 상태 삭제
        String key = "chat_active:" + chatRoomId + ":" + userId + ":" + userType;
        Boolean deleted = redisTemplate.delete(key);

        log.info("사용자 {}(타입:{}) 채팅방 {} 나가기 처리 완료 (삭제됨: {})",
                userId, userType, chatRoomId, deleted);
    }
    //입장 알림 메서드
    private void sendUserEnterNotification(Long chatRoomId, Long userId, Character userType) {
        try {
            // 입장 알림 메시지 생성
            ChatMessageDto enterNotification = ChatMessageDto.builder()
                    .type(ChatMessageDto.MessageType.JOIN)
                    .chatRoomId(chatRoomId)
                    .senderId(userId)
                    .senderType(userType)
                    .content("USER_ENTER")
                    .timestamp(LocalDateTime.now())
                    .build();

            // JSON으로 변환 후 Redis에 발행
            String jsonMessage = objectMapper.writeValueAsString(enterNotification);
            redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);

            log.debug("사용자 입장 알림 전송 완료: chatRoomId={}, userId={}", chatRoomId, userId);

        } catch (JsonProcessingException e) {
            log.error("입장 알림 전송 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}