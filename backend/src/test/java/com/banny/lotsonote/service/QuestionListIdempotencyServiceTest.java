package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.QuestionListItemMapper;
import com.banny.lotsonote.mapper.QuestionListMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.questionList.CreateQuestionListBody;
import com.banny.lotsonote.model.entity.QuestionList;
import com.banny.lotsonote.model.vo.questionList.CreateQuestionListVO;
import com.banny.lotsonote.service.impl.QuestionListServiceImpl;
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
public class QuestionListIdempotencyServiceTest {

    @Mock
    private QuestionListMapper questionListMapper;

    @Mock
    private QuestionListItemMapper questionListItemMapper;

    private QuestionListServiceImpl questionListService;

    @BeforeEach
    void setUp() {
        questionListService = new QuestionListServiceImpl();
        ReflectionTestUtils.setField(questionListService, "questionListMapper", questionListMapper);
        ReflectionTestUtils.setField(questionListService, "questionListItemMapper", questionListItemMapper);
    }

    @Test
    void createQuestionListShouldReturnExistingIdWhenRequestIsRepeated() {
        CreateQuestionListBody body = new CreateQuestionListBody();
        body.setName(" 算法冲刺 ");
        body.setType(2);
        body.setDescription("desc");

        QuestionList existingQuestionList = new QuestionList();
        existingQuestionList.setQuestionListId(9);
        existingQuestionList.setName("算法冲刺");
        existingQuestionList.setType(2);

        when(questionListMapper.findByNameAndType("算法冲刺", 2)).thenReturn(existingQuestionList);

        ApiResponse<CreateQuestionListVO> response = questionListService.createQuestionList(body);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(9, response.getData().getQuestionListId());
        verify(questionListMapper, never()).insert(any(QuestionList.class));
    }
}
