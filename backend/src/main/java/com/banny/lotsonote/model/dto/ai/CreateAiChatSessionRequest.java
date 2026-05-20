package com.banny.lotsonote.model.dto.ai;

import lombok.Data;

@Data
public class CreateAiChatSessionRequest {
    private AiSceneType sceneType;
    private AiBizType bizType;
    private String bizId;
    private String title;
    private AiContextSnapshot contextSnapshot;
    private Boolean forceRecreate;
}
