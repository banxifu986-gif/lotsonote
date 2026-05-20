package com.banny.lotsonote.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatSession {
    private Long sessionId;
    private Long userId;
    private String sceneType;
    private String bizType;
    private String bizId;
    private String title;
    private String contextSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
