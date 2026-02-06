package com.carhub.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
public class MyController {

    private final ChatClient chatClient;

    public MyController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ChatAI")
    public ModelAndView AI(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("ChatAI.html");
        return modelAndView;
    }

    @PostMapping("/ChatAI")
    String generation(@RequestBody Map<String, String> request) {
        String userInput = request.get("message");
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }
}