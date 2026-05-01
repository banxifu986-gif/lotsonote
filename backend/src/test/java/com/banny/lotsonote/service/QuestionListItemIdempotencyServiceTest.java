package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.QuestionListItemMapper;
import com.banny.lotsonote.mapper.QuestionListMapper;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.questionListItem.CreateQuestionListItemBody;
import com.banny.lotsonote.model.entity.QuestionListItem;
import com.banny.lotsonote.model.vo.questionListItem.CreateQuestionListItemVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.impl.QuestionListItemServiceImpl;
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
public class QuestionListItemIdempotencyServiceTest {

    @Mock
    private QuestionListItemMapper questionListItemMapper;

    @Mock
    private QuestionListMapper questionListMapper;

    @Mock
    private RequestScopeData requestScopeData;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private UserMapper userMapper;

    private QuestionListItemServiceImpl questionListItemService;

    @BeforeEach
    void setUp() {
        questionListItemService = new QuestionListItemServiceImpl();
        ReflectionTestUtils.setField(questionListItemService, "questionListItemMapper", questionListItemMapper);
        ReflectionTestUtils.setField(questionListItemService, "questionListMapper", questionListMapper);
        ReflectionTestUtils.setField(questionListItemService, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(questionListItemService, "noteMapper", noteMapper);
        ReflectionTestUtils.setField(questionListItemService, "userMapper", userMapper);
    }

    @Test
    void createQuestionListItemShouldReturnExistingRankWhenRequestIsRepeated() {
        CreateQuestionListItemBody body = new CreateQuestionListItemBody();
        body.setQuestionListId(11);
        body.setQuestionId(22);

        QuestionListItem existingItem = new QuestionListItem();
        existingItem.setQuestionListId(11);
        existingItem.setQuestionId(22);
        existingItem.setRank(3);

        when(questionListItemMapper.nextRank(11)).thenReturn(4);
        when(questionListItemMapper.insert(any(QuestionListItem.class))).thenReturn(0);
        when(questionListItemMapper.findByQuestionListIdAndQuestionId(11, 22)).thenReturn(existingItem);

        ApiResponse<CreateQuestionListItemVO> response = questionListItemService.createQuestionListItem(body);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(3, response.getData().getRank());
        verify(questionListItemMapper, never()).deleteByQuestionListIdAndQuestionId(11, 22);
    }
}
