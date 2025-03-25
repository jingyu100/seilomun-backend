    package com.yju.team2.seilomun.domain.chat.service;

    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.List;
    import java.util.Optional;
    import java.util.stream.Collectors;

    import com.yju.team2.seilomun.dto.ChatMessageDto;
    import com.yju.team2.seilomun.dto.ChatRoomDto;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.listener.ChannelTopic;
    import org.springframework.messaging.simp.user.SimpUserRegistry;
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
        private final SimpUserRegistry userRegistry;

        // 채팅방 생성 또는 기존 채팅방 찾기
        @Transactional
        public ChatRoomDto createOrGetChatRoom(Long customerId, Long sellerId) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));

            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));

            // 이미 존재하는 채팅방 확인
            Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findByCustomerIdAndSellerId(customerId, sellerId);

            // 기존 채팅방
            if (optionalChatRoom.isPresent()) {
                ChatRoom chatRoom = optionalChatRoom.get();

                // 해당 채팅방의 마지막 메시지
                List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom);

                String lastMessage;
                if (messages.isEmpty()) {
                    lastMessage = "";
                } else {
                    lastMessage = messages.get(0).getContent();
                }

                LocalDateTime lastMessageTime;
                if (messages.isEmpty()) {
                    lastMessageTime = chatRoom.getCreationAt();
                } else {
                    lastMessageTime = messages.get(0).getCreatedAt();
                }

                return ChatRoomDto.builder()
                        .id(chatRoom.getId())
                        .customerId(customerId)
                        .customerNickname(customer.getNickname())
                        .sellerId(sellerId)
                        .sellerStoreName(seller.getStoreName())
                        .createdAt(chatRoom.getCreationAt())
                        .lastMessage(lastMessage)
                        .lastMessageTime(lastMessageTime)
                        .build();
            }

            // 새 채팅방 생성
            ChatRoom newChatRoom = ChatRoom.builder()
                    .customer(customer)
                    .seller(seller)
                    .creationAt(LocalDateTime.now())
                    .build();

            chatRoomRepository.save(newChatRoom);

            return ChatRoomDto.builder()
                    .id(newChatRoom.getId())
                    .customerId(customerId)
                    .customerNickname(customer.getNickname())
                    .sellerId(sellerId)
                    .sellerStoreName(seller.getStoreName())
                    .createdAt(newChatRoom.getCreationAt())
                    .lastMessage("")
                    .lastMessageTime(newChatRoom.getCreationAt())
                    .build();
        }

        // 메시지 전송
        @Transactional
        public ChatMessageDto sendMessage(ChatMessageDto messageDto) {
            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getChatRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

            // 메시지 저장
            ChatMessage chatMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .senderType(messageDto.getSenderType())
                    .senderId(messageDto.getSenderId())
                    .content(messageDto.getContent())
                    .createdAt(LocalDateTime.now())
                    .isRead('N') // 임시로 차후 읽음표시는 나중에..
                    .build();

            chatMessageRepository.save(chatMessage);

            // 타임스탬프 설정
            messageDto.setTimestamp(chatMessage.getCreatedAt());

            // Redis를 통해 메시지 발행
            try {
                // JSON 문자열로 직렬화
                String jsonMessage = objectMapper.writeValueAsString(messageDto);

                // Redis에 메시지 발행
                redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);

                log.debug("메시지 발행 완료: {}", jsonMessage);
            } catch (JsonProcessingException e) {
                log.error("메시지 발행 중 에러 발생: {}", e.getMessage(), e);
            }
            return messageDto;
        }
        
        // 채팅 방 찾기
        // 채팅방 마지막 메세지가져오는거랑 읽음 여부는 다음에 만들게 ㄱㄷㄱㄷ
        public List<ChatRoomDto> getChatRooms(Long id, Character userType) {
            List<ChatRoom> chatRoomList;
            if(userType.equals('C')){
                 chatRoomList = chatRoomRepository.findByCustomer_Id(id);
            }
            else {
                chatRoomList = chatRoomRepository.findBySeller_Id(id);
            }
            List<ChatRoomDto> chatRoomDtoList = new ArrayList<>();

            for (ChatRoom chatRoom : chatRoomList) {
                Long customerId = chatRoom.getCustomer().getId();
                String customerNickname = chatRoom.getCustomer().getNickname();
                Long sellerId = chatRoom.getSeller().getId();
                String sellerStoreName = chatRoom.getSeller().getStoreName();

                ChatRoomDto chatRoomDto = ChatRoomDto.builder()
                        .id(chatRoom.getId())
                        .customerId(customerId)
                        .customerNickname(customerNickname)
                        .sellerId(sellerId)
                        .sellerStoreName(sellerStoreName)
                        .createdAt(chatRoom.getCreationAt())
                        .build();

                chatRoomDtoList.add(chatRoomDto);
            }
            return chatRoomDtoList;
        }
        // 일단 채팅 가져오기
        public List<ChatMessageDto> getChatMessages(Long id, Character userType, Long userId) {
            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = chatRoomRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));


            Long receiverId;
            String senderNickname;
            if(userType.equals('C')){
                receiverId = chatRoom.getSeller().getId();
                senderNickname = chatRoom.getCustomer().getNickname();
            }
            else {
                receiverId = chatRoom.getCustomer().getId();
                senderNickname = chatRoom.getSeller().getStoreName();
            }

            List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoom_Id(chatRoom.getId());

            List<ChatMessageDto> chatMessageDtoList = new ArrayList<>();
            for (ChatMessage chatMessage : chatMessages) {

                ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                        .type(ChatMessageDto.MessageType.CHAT)
                        .chatRoomId(chatRoom.getId())
                        .senderId(userId)
                        .receiverId(receiverId)
                        .senderName(senderNickname)
                        .content(chatMessage.getContent())
                        .senderType(userType)
                        .timestamp(chatMessage.getCreatedAt())
                        .read(true)
                        .build();
                chatMessageDtoList.add(chatMessageDto);
            }
            chatMessageDtoList.sort(Comparator.comparing(ChatMessageDto::getTimestamp));
            return chatMessageDtoList;
        }
    }