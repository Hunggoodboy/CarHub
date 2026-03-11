package com.carhub.controller;

import com.carhub.dto.ChatRequest;
import com.carhub.service.ai.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatAiController {

    private final AIChatService AIChatService;

    @GetMapping("/ChatAI")
    public String getChatAI() {
        return "ChatAI";
    }
    
    @PostMapping("/ChatAI")
    public ResponseEntity<String> chat(@RequestBody ChatRequest chatRequest) throws InterruptedException {
        String aiChatContent = AIChatService.generateAnswer(chatRequest);
        return ResponseEntity.ok(aiChatContent);
    }

}
