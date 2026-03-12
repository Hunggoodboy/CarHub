package com.carhub.repository;

import com.carhub.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    ChatMessage findBySenderIdAndReceiverId(Long SenderId, Long ReceiverId);
}
