package com.carhub.controller;

import com.carhub.dto.ChatRequest;
import com.carhub.service.ai.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequiredArgsConstructor
public class ChatAiController {

    private final ChatService chatService;

//    @GetMapping("/ChatAI")
//    public ModelAndView getChatAI() {
//        return new ModelAndView("ChatAI");
//    }

    @PostMapping("/ChatAI")
    public String chat(@RequestBody ChatRequest chatRequest) {
        return chatService.generateAnswer(chatRequest);
    }

}
