package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.note.CreateNoteRequest;
import com.banny.lotsonote.model.entity.Note;
import com.banny.lotsonote.model.entity.Question;
import com.banny.lotsonote.model.vo.note.CreateNoteVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.impl.NoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NoteIdempotencyServiceTest {

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private UserService userService;

    @Mock
    private QuestionService questionService;

    @Mock
    private NoteLikeService noteLikeService;

    @Mock
    private CollectionNoteService collectionNoteService;

    @Mock
    private RequestScopeData requestScopeData;

    @Mock
    private CategoryService categoryService;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private NoteServiceImpl noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteServiceImpl();
        ReflectionTestUtils.setField(noteService, "noteMapper", noteMapper);
        ReflectionTestUtils.setField(noteService, "userService", userService);
        ReflectionTestUtils.setField(noteService, "questionService", questionService);
        ReflectionTestUtils.setField(noteService, "noteLikeService", noteLikeService);
        ReflectionTestUtils.setField(noteService, "collectionNoteService", collectionNoteService);
        ReflectionTestUtils.setField(noteService, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(noteService, "categoryService", categoryService);
        ReflectionTestUtils.setField(noteService, "questionMapper", questionMapper);
        ReflectionTestUtils.setField(noteService, "redisTemplate", redisTemplate);
    }

    @Test
    void createNoteShouldReturnExistingNoteWhenRequestIsRepeated() {
        CreateNoteRequest request = new CreateNoteRequest();
        request.setQuestionId(100);
        request.setContent("content");

        Question question = new Question();
        question.setQuestionId(100);

        Note existingNote = new Note();
        existingNote.setNoteId(88);
        existingNote.setAuthorId(10L);
        existingNote.setQuestionId(100);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(questionService.findById(100)).thenReturn(question);
        when(noteMapper.findByAuthorIdAndQuestionId(10L, 100)).thenReturn(existingNote);

        ApiResponse<CreateNoteVO> response = noteService.createNote(request);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(88, response.getData().getNoteId());
        verify(noteMapper, never()).insert(any(Note.class));
        verify(redisTemplate, never()).opsForZSet();
    }
}
