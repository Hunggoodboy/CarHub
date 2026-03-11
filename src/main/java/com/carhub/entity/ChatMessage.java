package com.carhub.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private MessageType messageType;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead;
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
