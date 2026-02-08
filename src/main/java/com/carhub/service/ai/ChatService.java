package com.carhub.service.ai;

import com.carhub.dto.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatClient  chatClient;
    public ChatService(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    public String generateAnswer(ChatRequest chatRequest) {
        return chatClient.prompt()
                .user(chatRequest.getQuestion())
                .call()
                .content();
    }
}
