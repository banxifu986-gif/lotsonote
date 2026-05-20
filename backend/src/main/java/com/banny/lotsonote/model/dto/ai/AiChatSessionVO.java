package com.banny.lotsonote.model.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatSessionVO {
    private String chatSessionId;
    private String title;
    private String sceneType;
    private String bizType;
    private String bizId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
