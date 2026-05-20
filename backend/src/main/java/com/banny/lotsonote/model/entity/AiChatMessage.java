package com.banny.lotsonote.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessage {
    private Long messageId;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
