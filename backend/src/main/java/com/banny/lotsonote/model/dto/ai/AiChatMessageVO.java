package com.banny.lotsonote.model.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageVO {
    private String chatMessageId;
    private String id;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
