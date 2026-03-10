package com.dto;


import com.entity.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String receiverName;
    private ChatMessage.MessageType messageType;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead = false;
    public static ChatMessageDTO fromEntity(ChatMessage entity, String senderName, String receiverName) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setSenderId(entity.getSenderId());
        dto.setReceiverId(entity.getReceiverId());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRead(entity.isRead());
        dto.setSenderName(senderName);
        dto.setReceiverName(receiverName);
        return dto;
    }
}
