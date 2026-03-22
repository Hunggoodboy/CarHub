package com.carhub.controller;


import com.carhub.dto.AppPrincipal;
import com.carhub.dto.Request.ChatMessageRequest;
import com.carhub.entity.ChatMessage;
import com.carhub.repository.ChatMessageRepository;
import com.carhub.service.ChatMessageService;
import com.carhub.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Controller
@AllArgsConstructor
public class ChatWsController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final UserService userService;
    @MessageMapping("chat.private")
    public void sendPrivateMessage(@RequestBody ChatMessageRequest chatMessageRequest) {
        chatMessageService.chatMessage(chatMessageRequest);
    }

    @MessageMapping("chat.history")
    public void getChatHistory(@RequestBody ChatMessageRequest chatMessageRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long senderId = chatMessageRequest.getSenderId();
        Long receiverId = chatMessageRequest.getReceiverId();
        chatMessageService.loadMessageHistory(senderId, receiverId);
    }
}
