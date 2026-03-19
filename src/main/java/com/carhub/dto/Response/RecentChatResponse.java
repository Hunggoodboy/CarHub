package com.carhub.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentChatResponse {
    private Long partnerId;      // ID của người đang chat với mình
    private String partnerName;  // Tên hiển thị của họ
    private String partnerAvatar; // (Tùy chọn) Ảnh đại diện
    private String lastMessage;  // Nội dung tin nhắn cuối
    private LocalDateTime sentAt;
}
