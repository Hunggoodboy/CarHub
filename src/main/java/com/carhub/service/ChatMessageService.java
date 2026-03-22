package com.carhub.service;

import com.carhub.dto.Request.ChatMessageRequest;
import com.carhub.dto.Response.ChatMessageResponse;
import com.carhub.dto.Response.RecentChatResponse;
import com.carhub.entity.ChatMessage;
import com.carhub.entity.User;
import com.carhub.repository.ChatMessageRepository;
import com.carhub.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserRepository userRepository;
    public void saveChatMessage(ChatMessageRequest chatMessageRequest) {
        // Lưu tin nhắn vào cơ sở dữ liệu
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(userRepository.findUserById(chatMessageRequest.getSenderId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi")));
        chatMessage.setReceiver(userRepository.findUserById(chatMessageRequest.getReceiverId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận")));
        chatMessage.setContent(chatMessageRequest.getContent());
        chatMessage.setSentAt(LocalDateTime.now());
        chatMessageRepository.save(chatMessage);
    }

    public void loadMessageHistory(Long userId1, Long userId2) {
        User sender = userRepository.findUserById(userId1).orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));
        List<ChatMessage> chatMessages = chatMessageRepository.findAllBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderBySentAtAsc(userId1, userId2, userId2, userId1);
        List<ChatMessageResponse> responses = chatMessages.stream().map(chatMessage -> {
            ChatMessageResponse response = new ChatMessageResponse();
            response.setSenderId(chatMessage.getSender().getId());
            response.setReceiverId(chatMessage.getReceiver().getId());
            response.setSenderName(sender.getFullName());
            response.setContent(chatMessage.getContent());
            response.setSentAt(LocalDateTime.now());
            return response;
        }).toList();

        String receiverUserName = userRepository.findUserById(userId2).orElseThrow().getUsername();
        simpMessagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/private",
                responses
        );

        simpMessagingTemplate.convertAndSendToUser(
                receiverUserName,
                "/queue/private",
                responses
        );
    }

    public void chatMessage(ChatMessageRequest chatMessageRequest) {
        User sender = userRepository.findUserById(chatMessageRequest.getSenderId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));
        User receiver = userRepository.findUserById(chatMessageRequest.getReceiverId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận"));
        saveChatMessage(chatMessageRequest);
        ChatMessageResponse response = ChatMessageResponse.builder()
                        .receiverId(chatMessageRequest.getReceiverId())
                        .senderId(chatMessageRequest.getSenderId())
                        .senderName(sender.getFullName())
                        .content(chatMessageRequest.getContent())
                        .sentAt(LocalDateTime.now())
                        .build();
        System.out.println(sender.getUsername());
        System.out.println(receiver.getUsername());
        simpMessagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/private",
                response
        );

        simpMessagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/private",
                response
        );
    }
    public List<RecentChatResponse> getRecentChats(Long currentUserId) {
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentChatsByUserId(currentUserId);

        return recentMessages.stream().map(msg -> {
            // Xác định ai là "đối tác" (partner) trong tin nhắn này
            User partner = msg.getSender().getId().equals(currentUserId)
                    ? msg.getReceiver()
                    : msg.getSender();

            return RecentChatResponse.builder()
                    .partnerId(partner.getId())
                    .partnerName(partner.getFullName())
                    .lastMessage(msg.getContent())
                    .sentAt(msg.getSentAt())
                    .build();
        }).toList();
    }

    public List<ChatMessageResponse> getHistoryWithPagination(Long userId1, Long userId2, int page, int size) {
        Pageable pageable = (Pageable) PageRequest.of(page, size, Sort.by("sentAt").descending());

        Page<ChatMessage> messagePage = chatMessageRepository.findChatHistoryWithPagination(userId1, userId2, pageable);

        return messagePage.getContent().stream().map(chatMessage -> {
            ChatMessageResponse response = new ChatMessageResponse();
            response.setSenderId(chatMessage.getSender().getId());
            response.setReceiverId(chatMessage.getReceiver().getId());
            response.setSenderName(chatMessage.getSender().getFullName());
            response.setContent(chatMessage.getContent());
            response.setSentAt(chatMessage.getSentAt());

            return response;
        }).toList();
    }
}
