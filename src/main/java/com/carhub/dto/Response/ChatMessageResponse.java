package com.carhub.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
}
