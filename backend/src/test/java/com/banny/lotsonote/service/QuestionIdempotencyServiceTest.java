package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.CategoryMapper;
import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.question.CreateQuestionBody;
import com.banny.lotsonote.model.entity.Category;
import com.banny.lotsonote.model.entity.Question;
import com.banny.lotsonote.model.vo.question.CreateQuestionVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionIdempotencyServiceTest {

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private RequestScopeData requestScopeData;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private CategoryService categoryService;

    private QuestionServiceImpl questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl();
        ReflectionTestUtils.setField(questionService, "questionMapper", questionMapper);
        ReflectionTestUtils.setField(questionService, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(questionService, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(questionService, "noteMapper", noteMapper);
        ReflectionTestUtils.setField(questionService, "categoryService", categoryService);
    }

    @Test
    void createQuestionShouldReturnExistingQuestionWhenRequestIsRepeated() {
        CreateQuestionBody body = new CreateQuestionBody();
        body.setCategoryId(7);
        body.setTitle(" 二叉树遍历 ");
        body.setDifficulty(2);
        body.setExamPoint("树");

        Category category = new Category();
        category.setCategoryId(7);

        Question existingQuestion = new Question();
        existingQuestion.setQuestionId(15);
        existingQuestion.setCategoryId(7);
        existingQuestion.setTitle("二叉树遍历");

        when(categoryMapper.findById(7)).thenReturn(category);
        when(questionMapper.findByTitle("二叉树遍历")).thenReturn(existingQuestion);

        ApiResponse<CreateQuestionVO> response = questionService.createQuestion(body);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(15, response.getData().getQuestionId());
        verify(questionMapper, never()).insert(any(Question.class));
    }
}
