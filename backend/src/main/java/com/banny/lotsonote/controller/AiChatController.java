package com.banny.lotsonote.controller;

import com.banny.lotsonote.annotation.NeedLogin;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.ai.AiChatHistoryVO;
import com.banny.lotsonote.model.dto.ai.AiChatMessageVO;
import com.banny.lotsonote.model.dto.ai.AiChatSessionVO;
import com.banny.lotsonote.model.dto.ai.CreateAiChatMessageRequest;
import com.banny.lotsonote.model.dto.ai.CreateAiChatSessionRequest;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.AiChatService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private RequestScopeData requestScopeData;

    @NeedLogin
    @GetMapping("/chat-sessions")
    public ApiResponse<List<AiChatSessionVO>> listChatSessions() {
        return ApiResponseUtil.success("获取 AI 会话成功", aiChatService.listChatSessions(requestScopeData.getUserId()));
    }

    @NeedLogin
    @PostMapping("/chat-sessions")
    public ApiResponse<AiChatSessionVO> createChatSession(@Valid @RequestBody CreateAiChatSessionRequest request) {
        return ApiResponseUtil.success("创建 AI 会话成功", aiChatService.createChatSession(requestScopeData.getUserId(), request));
    }

    @NeedLogin
    @GetMapping("/chat-sessions/{sessionId}/messages")
    public ApiResponse<AiChatHistoryVO> getChatMessages(@PathVariable String sessionId) {
        return ApiResponseUtil.success("获取 AI 会话消息成功", aiChatService.getChatMessages(requestScopeData.getUserId(), sessionId));
    }

    @NeedLogin
    @PostMapping("/chat-messages")
    public ApiResponse<AiChatMessageVO> createChatMessage(@Valid @RequestBody CreateAiChatMessageRequest request) {
        return ApiResponseUtil.success("发送 AI 消息成功", aiChatService.createChatMessage(requestScopeData.getUserId(), request));
    }
}
