package com.banny.lotsonote.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAiChatMessageRequest {
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "content 不能为空")
    private String content;
}
