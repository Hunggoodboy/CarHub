package com.carhub.controller;


import com.carhub.dto.AppPrincipal;
import com.carhub.dto.ChatMessageDTO;
import com.carhub.entity.ChatMessage;
import com.carhub.repository.ChatMessageRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@AllArgsConstructor
public class ChatWsController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/chat/{receiverId}")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessageDTO chatMessage, @PathVariable Long receiverId, Authentication authentication) {
        AppPrincipal sender = (AppPrincipal) authentication.getPrincipal();
        ChatMessage message = new ChatMessage();
        message.setSenderId(sender.getUserId());
        message.setReceiverId(receiverId);
        message.setMessageType(chatMessage.getMessageType());
        message.setContent(chatMessage.getContent());
        message.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(message);
        ChatMessageDTO response = ChatMessageDTO.fromEntity(message, chatMessage.getSenderName(), chatMessage.getReceiverName());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(response.getReceiverId()), "/queue/consult", response
        );
        messagingTemplate.convertAndSendToUser(
                String.valueOf(sender.getUserId()), "/queue/consult", response
        );
        return ResponseEntity.ok(response);
    }
}
