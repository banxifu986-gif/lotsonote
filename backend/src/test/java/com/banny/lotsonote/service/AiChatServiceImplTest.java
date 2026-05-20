package com.banny.lotsonote.service;

import com.banny.lotsonote.config.AiModelProperties;
import com.banny.lotsonote.mapper.CategoryMapper;
import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.mapper.QuestionSolutionMapper;
import com.banny.lotsonote.model.dto.ai.AiBizType;
import com.banny.lotsonote.model.dto.ai.AiContextSnapshot;
import com.banny.lotsonote.model.dto.ai.AiSceneType;
import com.banny.lotsonote.model.entity.AiChatSession;
import com.banny.lotsonote.model.entity.Category;
import com.banny.lotsonote.model.entity.Note;
import com.banny.lotsonote.model.entity.Question;
import com.banny.lotsonote.model.entity.QuestionSolution;
import com.banny.lotsonote.service.impl.AiChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AiChatServiceImplTest {

    @Test
    public void buildQuestionTutorContextShouldExposeReferenceSolutionSeparately() {
        AiChatServiceImpl service = new AiChatServiceImpl();
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionSolutionMapper questionSolutionMapper = mock(QuestionSolutionMapper.class);
        NoteMapper noteMapper = mock(NoteMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);

        ReflectionTestUtils.setField(service, "questionMapper", questionMapper);
        ReflectionTestUtils.setField(service, "questionSolutionMapper", questionSolutionMapper);
        ReflectionTestUtils.setField(service, "noteMapper", noteMapper);
        ReflectionTestUtils.setField(service, "categoryMapper", categoryMapper);

        Question question = new Question();
        question.setQuestionId(200059);
        question.setCategoryId(2);
        question.setTitle("你知道的线程同步的方式有哪些？");
        question.setDifficulty(2);
        question.setExamPoint("线程同步的机制、方法");
        when(questionMapper.findById(200059)).thenReturn(question);

        QuestionSolution questionSolution = new QuestionSolution();
        questionSolution.setQuestionId(200059);
        questionSolution.setContent("可以从 synchronized、Lock、volatile 的适用边界来梳理。");
        when(questionSolutionMapper.findByQuestionId(200059)).thenReturn(questionSolution);

        Note note = new Note();
        note.setContent("我总是混淆 synchronized 和 volatile。");
        when(noteMapper.findByAuthorIdAndQuestionId(1L, 200059)).thenReturn(note);

        Category category = new Category();
        category.setCategoryId(2);
        category.setName("并发");
        category.setParentCategoryId(1);
        when(categoryMapper.findById(2)).thenReturn(category);

        Category parentCategory = new Category();
        parentCategory.setCategoryId(1);
        parentCategory.setName("Java");
        parentCategory.setParentCategoryId(0);
        when(categoryMapper.findById(1)).thenReturn(parentCategory);

        AiContextSnapshot snapshot = (AiContextSnapshot) ReflectionTestUtils.invokeMethod(
                service,
                "buildQuestionTutorContext",
                1L,
                "200059",
                null
        );

        assertNotNull(snapshot);
        assertEquals("你知道的线程同步的方式有哪些？", snapshot.getQuestionContent());
        assertEquals("Java > 并发", snapshot.getContentPath());
        assertEquals("线程同步的机制、方法", snapshot.getExamPoint());
        assertEquals("可以从 synchronized、Lock、volatile 的适用边界来梳理。", snapshot.getReferenceSolution());
        assertEquals("我总是混淆 synchronized 和 volatile。", snapshot.getNoteContent());
        assertTrue(snapshot.getDescription().contains("难度: 中等"));
        assertTrue(snapshot.getDescription().contains("考点: 线程同步的机制、方法"));
        assertFalse(snapshot.getDescription().contains("参考解析"));
    }

    @Test
    public void buildSystemPromptShouldUseQuestionTutorStructure() {
        AiChatServiceImpl service = new AiChatServiceImpl();
        AiChatSession session = new AiChatSession();
        session.setSceneType(AiSceneType.QUESTION_TUTOR.name());

        String prompt = (String) ReflectionTestUtils.invokeMethod(service, "buildSystemPrompt", session);

        assertNotNull(prompt);
        assertTrue(prompt.contains("题目学习教练"));
        assertTrue(prompt.contains("结论："));
        assertTrue(prompt.contains("思路："));
        assertTrue(prompt.contains("考点："));
        assertTrue(prompt.contains("自检："));
        assertTrue(prompt.contains("不要直接判定对错"));
    }

    @Test
    public void buildContextPromptShouldPrioritizeReferenceSolutionForQuestionTutor() {
        AiChatServiceImpl service = new AiChatServiceImpl();
        AiChatSession session = new AiChatSession();
        session.setSceneType(AiSceneType.QUESTION_TUTOR.name());
        session.setBizType(AiBizType.QUESTION.name());
        session.setBizId("200059");

        AiContextSnapshot snapshot = new AiContextSnapshot();
        snapshot.setQuestionContent("你知道的线程同步的方式有哪些？");
        snapshot.setContentPath("Java > 并发");
        snapshot.setExamPoint("线程同步的机制、方法");
        snapshot.setReferenceSolution("先按 synchronized、Lock、volatile 分类，再比较适用场景。");
        snapshot.setNoteContent("我还不确定 volatile 能不能保证复合操作安全。");
        snapshot.setDescription("难度: 中等；考点: 线程同步的机制、方法");
        session.setContextSnapshot("{\"questionContent\":\"你知道的线程同步的方式有哪些？\",\"contentPath\":\"Java > 并发\",\"examPoint\":\"线程同步的机制、方法\",\"referenceSolution\":\"先按 synchronized、Lock、volatile 分类，再比较适用场景。\",\"noteContent\":\"我还不确定 volatile 能不能保证复合操作安全。\",\"description\":\"难度: 中等；考点: 线程同步的机制、方法\"}");

        String contextPrompt = (String) ReflectionTestUtils.invokeMethod(service, "buildContextPrompt", session);

        assertNotNull(contextPrompt);
        assertTrue(contextPrompt.contains("参考解析:\n先按 synchronized、Lock、volatile 分类，再比较适用场景。"));
        assertTrue(contextPrompt.contains("用户笔记:\n我还不确定 volatile 能不能保证复合操作安全。"));
        assertTrue(contextPrompt.contains("题干:\n你知道的线程同步的方式有哪些？"));
        assertTrue(contextPrompt.contains("分类路径:\nJava > 并发"));
        assertTrue(contextPrompt.contains("上下文使用规则：优先依据“参考解析”"));
        assertTrue(contextPrompt.contains("如果用户要求判对错，只能输出检查角度"));
    }

    @Test
    public void buildContextPromptShouldMarkMissingReferenceSolutionForQuestionTutor() {
        AiChatServiceImpl service = new AiChatServiceImpl();
        AiChatSession session = new AiChatSession();
        session.setSceneType(AiSceneType.QUESTION_TUTOR.name());
        session.setBizType(AiBizType.QUESTION.name());
        session.setBizId("200060");
        session.setContextSnapshot("{\"questionContent\":\"HTTP请求方式有哪些？\",\"contentPath\":\"计算机网络 > HTTP\",\"examPoint\":\"HTTP请求方法\",\"noteContent\":\"GET、POST、PUT、DELETE\",\"description\":\"难度: 简单；考点: HTTP请求方法\"}");

        String contextPrompt = (String) ReflectionTestUtils.invokeMethod(service, "buildContextPrompt", session);

        assertNotNull(contextPrompt);
        assertTrue(contextPrompt.contains("参考解析：无（未上传）"));
        assertTrue(contextPrompt.contains("禁止使用“根据参考解析”"));
        assertTrue(contextPrompt.contains("用户笔记:\nGET、POST、PUT、DELETE"));
    }

    @Test
    public void buildSystemPromptShouldKeepGeneralChatPromptUntouched() {
        AiChatServiceImpl service = new AiChatServiceImpl();
        AiModelProperties aiModelProperties = new AiModelProperties();
        aiModelProperties.setSystemPrompt("通用聊天提示词");
        ReflectionTestUtils.setField(service, "aiModelProperties", aiModelProperties);

        AiChatSession session = new AiChatSession();
        session.setSceneType(AiSceneType.GENERAL_CHAT.name());

        String prompt = (String) ReflectionTestUtils.invokeMethod(service, "buildSystemPrompt", session);

        assertEquals("通用聊天提示词", prompt);
    }
}
