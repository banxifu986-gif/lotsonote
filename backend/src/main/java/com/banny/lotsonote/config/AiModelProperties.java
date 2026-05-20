package com.banny.lotsonote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.model")
public class AiModelProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutMillis = 30000;
    private String systemPrompt = "你是 LotsoNote 的学习助手。请结合用户提供的上下文，给出准确、简洁、可执行的学习建议，不要编造未提供的事实。";
}
