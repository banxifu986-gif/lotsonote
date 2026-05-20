package com.banny.lotsonote.service;

import com.banny.lotsonote.model.dto.ai.AiChatHistoryVO;
import com.banny.lotsonote.model.dto.ai.AiChatMessageVO;
import com.banny.lotsonote.model.dto.ai.AiChatSessionVO;
import com.banny.lotsonote.model.dto.ai.CreateAiChatMessageRequest;
import com.banny.lotsonote.model.dto.ai.CreateAiChatSessionRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface AiChatService {
    List<AiChatSessionVO> listChatSessions(Long userId);

    AiChatSessionVO createChatSession(Long userId, CreateAiChatSessionRequest request);

    AiChatHistoryVO getChatMessages(Long userId, String sessionId);

    AiChatMessageVO createChatMessage(Long userId, CreateAiChatMessageRequest request);
}
