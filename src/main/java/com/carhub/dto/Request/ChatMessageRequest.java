package com.carhub.dto.Request;


import com.carhub.entity.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime createdAt;
}
