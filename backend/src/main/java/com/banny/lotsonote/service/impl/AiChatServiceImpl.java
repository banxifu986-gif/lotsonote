package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.config.AiModelProperties;
import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.mapper.AiChatMessageMapper;
import com.banny.lotsonote.mapper.AiChatSessionMapper;
import com.banny.lotsonote.mapper.CategoryMapper;
import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.mapper.QuestionSolutionMapper;
import com.banny.lotsonote.model.dto.ai.AiBizType;
import com.banny.lotsonote.model.dto.ai.AiChatHistoryVO;
import com.banny.lotsonote.model.dto.ai.AiChatMessageVO;
import com.banny.lotsonote.model.dto.ai.AiChatSessionVO;
import com.banny.lotsonote.model.dto.ai.AiContextSnapshot;
import com.banny.lotsonote.model.dto.ai.AiSceneType;
import com.banny.lotsonote.model.dto.ai.CreateAiChatMessageRequest;
import com.banny.lotsonote.model.dto.ai.CreateAiChatSessionRequest;
import com.banny.lotsonote.model.entity.AiChatMessage;
import com.banny.lotsonote.model.entity.AiChatSession;
import com.banny.lotsonote.model.entity.Category;
import com.banny.lotsonote.model.entity.Note;
import com.banny.lotsonote.model.entity.Question;
import com.banny.lotsonote.model.entity.QuestionSolution;
import com.banny.lotsonote.service.AiChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String DEFAULT_SESSION_TITLE = "新对话";

    @Autowired
    private AiChatSessionMapper aiChatSessionMapper;

    @Autowired
    private AiChatMessageMapper aiChatMessageMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private QuestionSolutionMapper questionSolutionMapper;

    @Autowired
    private AiModelProperties aiModelProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public List<AiChatSessionVO> listChatSessions(Long userId) {
        List<AiChatSession> sessions = aiChatSessionMapper.findByUserId(userId);
        List<AiChatSessionVO> result = new ArrayList<>();
        for (AiChatSession session : sessions) {
            result.add(toSessionVO(session));
        }
        return result;
    }

    @Override
    public AiChatSessionVO createChatSession(Long userId, CreateAiChatSessionRequest request) {
        AiSceneType sceneType = resolveSceneType(request.getSceneType());
        AiBizType bizType = resolveBizType(request.getBizType());
        String bizId = trimToNull(request.getBizId());
        AiContextSnapshot contextSnapshot = enrichContextSnapshot(userId, sceneType, bizType, bizId, request.getContextSnapshot());
        String sessionTitle = resolveSessionTitle(request.getTitle(), contextSnapshot == null ? null : contextSnapshot.getTitle());

        if (!Boolean.TRUE.equals(request.getForceRecreate()) && shouldReuseSession(sceneType, bizType, bizId)) {
            AiChatSession existingSession = aiChatSessionMapper.findLatestByUserIdAndBiz(
                    userId,
                    sceneType.name(),
                    bizType.name(),
                    bizId
            );
            if (existingSession != null) {
                aiChatSessionMapper.updateContextAndTouch(
                        existingSession.getSessionId(),
                        sessionTitle,
                        writeContextSnapshot(contextSnapshot)
                );
                AiChatSession updatedSession = aiChatSessionMapper.findById(existingSession.getSessionId());
                return toSessionVO(updatedSession);
            }
        }

        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setSceneType(sceneType.name());
        session.setBizType(bizType.name());
        session.setBizId(bizId);
        session.setTitle(sessionTitle);
        session.setContextSnapshot(writeContextSnapshot(contextSnapshot));
        aiChatSessionMapper.insert(session);
        AiChatSession createdSession = aiChatSessionMapper.findById(session.getSessionId());
        return toSessionVO(createdSession);
    }

    @Override
    public AiChatHistoryVO getChatMessages(Long userId, String sessionId) {
        AiChatSession session = findOwnedSession(userId, sessionId);
        List<AiChatMessage> messages = aiChatMessageMapper.findBySessionId(session.getSessionId());
        AiChatHistoryVO historyVO = new AiChatHistoryVO();
        for (AiChatMessage message : messages) {
            historyVO.getMessages().add(toMessageVO(message));
        }
        return historyVO;
    }

    @Override
    public AiChatMessageVO createChatMessage(Long userId, CreateAiChatMessageRequest request) {
        AiChatSession session = findOwnedSession(userId, request.getSessionId());
        String content = trimToNull(request.getContent());
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("消息内容不能为空");
        }

        String sessionTitle = session.getTitle();
        if (!StringUtils.hasText(sessionTitle) || DEFAULT_SESSION_TITLE.equals(sessionTitle)) {
            sessionTitle = resolveSessionTitle(null, content);
            aiChatSessionMapper.updateTitleAndTouch(session.getSessionId(), sessionTitle);
            session.setTitle(sessionTitle);
        } else {
            aiChatSessionMapper.touch(session.getSessionId());
        }

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(session.getSessionId());
        userMessage.setRole("user");
        userMessage.setContent(content);
        aiChatMessageMapper.insert(userMessage);

        List<AiChatMessage> historyMessages = aiChatMessageMapper.findBySessionId(session.getSessionId());
        String assistantContent = callModel(session, historyMessages);

        AiChatMessage assistantMessage = new AiChatMessage();
        assistantMessage.setSessionId(session.getSessionId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(assistantContent);
        aiChatMessageMapper.insert(assistantMessage);

        List<AiChatMessage> latestMessages = aiChatMessageMapper.findBySessionId(session.getSessionId());
        if (latestMessages.isEmpty()) {
            throw new BusinessException("AI 消息保存失败");
        }
        AiChatMessage latestAssistantMessage = latestMessages.get(latestMessages.size() - 1);
        return toMessageVO(latestAssistantMessage);
    }

    private AiChatSession findOwnedSession(Long userId, String sessionId) {
        Long sessionIdValue = parseSessionId(sessionId);
        AiChatSession session = aiChatSessionMapper.findById(sessionIdValue);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("AI 会话不存在");
        }
        return session;
    }

    private Long parseSessionId(String sessionId) {
        try {
            return Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            throw new BusinessException("sessionId 非法");
        }
    }

    private AiSceneType resolveSceneType(AiSceneType sceneType) {
        return sceneType == null ? AiSceneType.GENERAL_CHAT : sceneType;
    }

    private AiBizType resolveBizType(AiBizType bizType) {
        return bizType == null ? AiBizType.USER_SPACE : bizType;
    }

    private String resolveSessionTitle(String title, String fallbackContent) {
        String normalizedTitle = trimToNull(title);
        if (StringUtils.hasText(normalizedTitle)) {
            return normalizedTitle.length() > 40 ? normalizedTitle.substring(0, 40) : normalizedTitle;
        }

        String content = trimToNull(fallbackContent);
        if (!StringUtils.hasText(content)) {
            return DEFAULT_SESSION_TITLE;
        }
        return content.length() > 40 ? content.substring(0, 40) : content;
    }

    private boolean shouldReuseSession(AiSceneType sceneType, AiBizType bizType, String bizId) {
        return sceneType == AiSceneType.QUESTION_TUTOR
                && bizType == AiBizType.QUESTION
                && StringUtils.hasText(bizId);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private AiContextSnapshot enrichContextSnapshot(Long userId,
                                                    AiSceneType sceneType,
                                                    AiBizType bizType,
                                                    String bizId,
                                                    AiContextSnapshot requestSnapshot) {
        if (sceneType == AiSceneType.QUESTION_TUTOR && bizType == AiBizType.QUESTION && StringUtils.hasText(bizId)) {
            return buildQuestionTutorContext(userId, bizId, requestSnapshot);
        }
        return requestSnapshot;
    }

    private AiContextSnapshot buildQuestionTutorContext(Long userId, String bizId, AiContextSnapshot requestSnapshot) {
        Integer questionId;
        try {
            questionId = Integer.parseInt(bizId);
        } catch (NumberFormatException e) {
            throw new BusinessException("题目场景 bizId 非法");
        }

        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        AiContextSnapshot snapshot = requestSnapshot == null ? new AiContextSnapshot() : requestSnapshot;
        snapshot.setTitle(question.getTitle());
        snapshot.setQuestionContent(question.getTitle());
        snapshot.setCategoryName(resolveCategoryName(question.getCategoryId()));
        snapshot.setContentPath(resolveCategoryPath(question.getCategoryId()));
        snapshot.setExamPoint(trimToNull(question.getExamPoint()));
        snapshot.setDescription(buildQuestionDescription(question));
        QuestionSolution questionSolution = questionSolutionMapper.findByQuestionId(questionId);
        snapshot.setReferenceSolution(questionSolution == null ? null : trimToNull(questionSolution.getContent()));

        Note userNote = noteMapper.findByAuthorIdAndQuestionId(userId, questionId);
        snapshot.setNoteContent(userNote == null ? null : trimToNull(userNote.getContent()));
        snapshot.setNoteDraft(null);
        snapshot.setRelatedTitles(Collections.emptyList());
        return snapshot;
    }

    private String buildQuestionDescription(Question question) {
        StringBuilder builder = new StringBuilder();
        if (question.getDifficulty() != null) {
            builder.append("难度: ").append(resolveDifficultyLabel(question.getDifficulty()));
        }
        if (StringUtils.hasText(question.getExamPoint())) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append("考点: ").append(question.getExamPoint().trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String resolveDifficultyLabel(Integer difficulty) {
        return switch (difficulty) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> String.valueOf(difficulty);
        };
    }

    private String resolveCategoryName(Integer categoryId) {
        Category category = categoryMapper.findById(categoryId);
        return category == null ? null : trimToNull(category.getName());
    }

    private String resolveCategoryPath(Integer categoryId) {
        List<String> pathSegments = new ArrayList<>();
        Integer currentCategoryId = categoryId;
        while (currentCategoryId != null && currentCategoryId > 0) {
            Category category = categoryMapper.findById(currentCategoryId);
            if (category == null) {
                break;
            }
            if (StringUtils.hasText(category.getName())) {
                pathSegments.add(0, category.getName().trim());
            }
            Integer parentCategoryId = category.getParentCategoryId();
            if (parentCategoryId == null || parentCategoryId <= 0 || parentCategoryId.equals(currentCategoryId)) {
                break;
            }
            currentCategoryId = parentCategoryId;
        }
        if (pathSegments.isEmpty()) {
            return null;
        }
        return String.join(" > ", pathSegments);
    }

    private String writeContextSnapshot(AiContextSnapshot contextSnapshot) {
        if (contextSnapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(contextSnapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("AI 会话上下文保存失败");
        }
    }

    private AiContextSnapshot readContextSnapshot(String contextSnapshot) {
        if (!StringUtils.hasText(contextSnapshot)) {
            return null;
        }
        try {
            return objectMapper.readValue(contextSnapshot, AiContextSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("AI 会话上下文解析失败");
        }
    }

    private String callModel(AiChatSession session, List<AiChatMessage> historyMessages) {
        if (!StringUtils.hasText(aiModelProperties.getApiKey())) {
            throw new BusinessException("AI_MODEL_API_KEY 未配置");
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", aiModelProperties.getModel());
        ArrayNode messagesNode = requestBody.putArray("messages");
        messagesNode.addObject()
                .put("role", "system")
                .put("content", buildSystemPrompt(session));

        String contextPrompt = buildContextPrompt(session);
        if (StringUtils.hasText(contextPrompt)) {
            messagesNode.addObject()
                    .put("role", "system")
                    .put("content", contextPrompt);
        }

        for (AiChatMessage historyMessage : historyMessages) {
            if (!StringUtils.hasText(historyMessage.getContent())) {
                continue;
            }
            messagesNode.addObject()
                    .put("role", historyMessage.getRole())
                    .put("content", historyMessage.getContent());
        }

        requestBody.put("temperature", 0.7);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildChatCompletionUrl()))
                .timeout(Duration.ofMillis(aiModelProperties.getTimeoutMillis()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiModelProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new BusinessException(extractErrorMessage(response.body()));
            }
            JsonNode responseBody = objectMapper.readTree(response.body());
            String content = responseBody.path("choices").path(0).path("message").path("content").asText();
            String normalizedContent = trimToNull(content);
            if (!StringUtils.hasText(normalizedContent)) {
                throw new BusinessException("AI 返回内容为空");
            }
            return normalizedContent;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI 服务调用被中断");
        } catch (IOException e) {
            throw new BusinessException("AI 服务调用失败，请稍后重试");
        }
    }

    private String buildChatCompletionUrl() {
        String baseUrl = aiModelProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat/completions";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String errorMessage = root.path("error").path("message").asText();
            if (StringUtils.hasText(errorMessage)) {
                return "AI 服务调用失败: " + errorMessage;
            }
        } catch (JsonProcessingException ignored) {
            // ignore
        }
        return "AI 服务调用失败，请稍后重试";
    }

    private String buildSystemPrompt(AiChatSession session) {
        if (AiSceneType.QUESTION_TUTOR.name().equals(session.getSceneType())) {
            return """
                    你是 LotsoNote 的题目学习教练，只负责辅导用户理解题目、梳理思路、定位考点和安排自检。
                    你只能基于当前会话提供的题干、分类路径、参考解析、用户笔记和对话内容回答，不能编造额外事实。
                    参考解析优先级最高；如果有参考解析，讲解必须优先围绕解析展开，并说明它对应的思路与考点。
                    如果没有参考解析，只能基于题干、分类路径和用户笔记给出学习辅导，不能假装自己掌握标准答案。
                    如果上下文明确写了“参考解析：无（未上传）”，禁止使用“根据参考解析”“参考解析明确指出”“参考答案显示”这类表述。
                    当用户追问“我这样写对吗”“我对吗”“我是不是做错了”这类判题问题时，不要直接判定对错，不要承诺系统已完成判题；只给出可检查的角度、遗漏点和下一步自检方法。
                    每次回答必须固定使用以下四段标题，且按这个顺序输出：
                    结论：
                    思路：
                    考点：
                    自检：
                    回答要简洁、具体、可执行；如果信息不足，要明确指出缺失信息落在哪一段，不要省略标题。
                    """;
        }
        return aiModelProperties.getSystemPrompt();
    }

    private String buildContextPrompt(AiChatSession session) {
        if (AiSceneType.QUESTION_TUTOR.name().equals(session.getSceneType())) {
            return buildQuestionTutorContextPrompt(session);
        }

        AiContextSnapshot contextSnapshot = readContextSnapshot(session.getContextSnapshot());
        if (contextSnapshot == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("当前会话来自 LotsoNote。\n");
        builder.append("场景: ").append(session.getSceneType()).append('\n');
        builder.append("业务类型: ").append(session.getBizType()).append('\n');
        if (StringUtils.hasText(session.getBizId())) {
            builder.append("业务 ID: ").append(session.getBizId()).append('\n');
        }
        appendContextLine(builder, "标题", contextSnapshot.getTitle());
        appendContextLine(builder, "分类", contextSnapshot.getCategoryName());
        appendContextLine(builder, "路径", contextSnapshot.getContentPath());
        appendContextLine(builder, "题目内容", contextSnapshot.getQuestionContent());
        appendContextLine(builder, "笔记草稿", contextSnapshot.getNoteDraft());
        appendContextLine(builder, "笔记内容", contextSnapshot.getNoteContent());
        appendContextLine(builder, "补充说明", contextSnapshot.getDescription());
        if (contextSnapshot.getRelatedTitles() != null && !contextSnapshot.getRelatedTitles().isEmpty()) {
            builder.append("相关标题: ").append(String.join("、", contextSnapshot.getRelatedTitles())).append('\n');
        }
        builder.append("请仅基于这些上下文和对话内容回答，不要假设系统中存在未提供的数据。");
        return builder.toString();
    }

    private String buildQuestionTutorContextPrompt(AiChatSession session) {
        AiContextSnapshot contextSnapshot = readContextSnapshot(session.getContextSnapshot());
        if (contextSnapshot == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("当前会话来自 LotsoNote。\n");
        builder.append("场景: ").append(session.getSceneType()).append('\n');
        builder.append("业务类型: ").append(session.getBizType()).append('\n');
        if (StringUtils.hasText(session.getBizId())) {
            builder.append("业务 ID: ").append(session.getBizId()).append('\n');
        }
        String referenceSolution = trimToNull(contextSnapshot.getReferenceSolution());
        if (StringUtils.hasText(referenceSolution)) {
            appendQuestionTutorContextSection(builder, "参考解析", referenceSolution);
        } else {
            builder.append("参考解析：无（未上传）\n");
            builder.append("约束：本题未上传参考解析，禁止使用“根据参考解析”“参考解析明确指出”“参考答案显示”这类表述。\n");
        }
        appendQuestionTutorContextSection(builder, "用户笔记", contextSnapshot.getNoteContent());
        appendQuestionTutorContextSection(builder, "题干", contextSnapshot.getQuestionContent());
        appendQuestionTutorContextSection(builder, "分类路径", contextSnapshot.getContentPath());
        appendQuestionTutorContextSection(builder, "考点", contextSnapshot.getExamPoint());
        appendQuestionTutorContextSection(builder, "补充说明", contextSnapshot.getDescription());
        builder.append("上下文使用规则：优先依据“参考解析”，没有参考解析时再回退到“题干”和“用户笔记”。\n");
        builder.append("如果用户要求判对错，只能输出检查角度，不得声称系统已经判题。");
        return builder.toString();
    }

    private void appendQuestionTutorContextSection(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(label).append(":\n");
        builder.append(value.trim()).append('\n');
    }

    private void appendContextLine(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private AiChatSessionVO toSessionVO(AiChatSession session) {
        AiChatSessionVO sessionVO = new AiChatSessionVO();
        sessionVO.setChatSessionId(String.valueOf(session.getSessionId()));
        sessionVO.setTitle(session.getTitle());
        sessionVO.setSceneType(session.getSceneType());
        sessionVO.setBizType(session.getBizType());
        sessionVO.setBizId(session.getBizId());
        sessionVO.setCreatedAt(session.getCreatedAt());
        sessionVO.setUpdatedAt(session.getUpdatedAt());
        return sessionVO;
    }

    private AiChatMessageVO toMessageVO(AiChatMessage message) {
        AiChatMessageVO messageVO = new AiChatMessageVO();
        messageVO.setChatMessageId(String.valueOf(message.getMessageId()));
        messageVO.setId(String.valueOf(message.getMessageId()));
        messageVO.setSessionId(String.valueOf(message.getSessionId()));
        messageVO.setRole(message.getRole());
        messageVO.setContent(message.getContent());
        messageVO.setCreatedAt(message.getCreatedAt());
        return messageVO;
    }
}
