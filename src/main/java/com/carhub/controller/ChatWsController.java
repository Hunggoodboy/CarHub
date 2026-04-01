package com.carhub.controller;


import com.carhub.dto.Request.ChatMessageRequest;
import com.carhub.repository.ChatMessageRepository;
import com.carhub.service.message.ChatMessageService;
import com.carhub.service.authentication.UserService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

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
