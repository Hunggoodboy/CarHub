package com.carhub.controller;

import com.carhub.dto.Response.ChatMessageResponse;
import com.carhub.dto.Response.RecentChatResponse;
import com.carhub.service.ChatMessageService;
import com.carhub.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@AllArgsConstructor
public class ChatRestController {
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    // 1. API lấy danh sách người đã chat (Cột bên trái)
    @GetMapping("/recent")
    public ResponseEntity<List<RecentChatResponse>> getRecentChats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = userService.getId(authentication);
        List<RecentChatResponse> recentChats = chatMessageService.getRecentChats(currentUserId);
        return ResponseEntity.ok(recentChats);
    }

    // 2. API lấy lịch sử chat với 1 người cụ thể (Khung bên phải)
    @GetMapping("/history/{partnerId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = userService.getId(authentication);
        List<ChatMessageResponse> history = chatMessageService.getHistoryWithPagination(currentUserId, partnerId, page, size);
        return ResponseEntity.ok(history);
    }
}
