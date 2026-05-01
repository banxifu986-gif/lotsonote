package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.CommentLikeMapper;
import com.banny.lotsonote.mapper.CommentMapper;
import com.banny.lotsonote.mapper.NoteLikeMapper;
import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.EmptyVO;
import com.banny.lotsonote.model.entity.Comment;
import com.banny.lotsonote.model.entity.Note;
import com.banny.lotsonote.model.entity.NoteLike;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.impl.CommentServiceImpl;
import com.banny.lotsonote.service.impl.NoteLikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LikeIdempotencyServiceTest {

    @Mock
    private NoteLikeMapper noteLikeMapper;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private RequestScopeData requestScopeData;

    @Mock
    private MessageService messageService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CommentLikeMapper commentLikeMapper;

    private NoteLikeServiceImpl noteLikeService;
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        noteLikeService = new NoteLikeServiceImpl(noteLikeMapper, noteMapper, requestScopeData, messageService);
        commentService = new CommentServiceImpl(commentMapper, noteMapper, userMapper, commentLikeMapper, messageService, requestScopeData);
    }

    @Test
    void likeNoteShouldNotIncreaseCountWhenAlreadyLiked() {
        Note note = new Note();
        note.setNoteId(1);
        note.setAuthorId(2L);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(noteMapper.findById(1)).thenReturn(note);
        when(noteLikeMapper.insert(any(NoteLike.class))).thenReturn(0);

        ApiResponse<EmptyVO> response = noteLikeService.likeNote(1);

        assertEquals(200, response.getCode());
        verify(noteMapper, never()).likeNote(1);
        verify(messageService, never()).createMessage(any());
    }

    @Test
    void unlikeNoteShouldNotDecreaseCountWhenAlreadyUnliked() {
        Note note = new Note();
        note.setNoteId(1);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(noteMapper.findById(1)).thenReturn(note);
        when(noteLikeMapper.findByUserIdAndNoteId(10L, 1)).thenReturn(null);

        ApiResponse<EmptyVO> response = noteLikeService.unlikeNote(1);

        assertEquals(200, response.getCode());
        verify(noteMapper, never()).unlikeNote(1);
    }

    @Test
    void likeCommentShouldNotIncreaseCountWhenAlreadyLiked() {
        Comment comment = new Comment();
        comment.setCommentId(1);
        comment.setAuthorId(2L);
        comment.setNoteId(3);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(commentMapper.findById(1)).thenReturn(comment);
        when(commentLikeMapper.insert(any())).thenReturn(0);

        ApiResponse<EmptyVO> response = commentService.likeComment(1);

        assertEquals(200, response.getCode());
        verify(commentMapper, never()).incrementLikeCount(1);
        verify(messageService, never()).createMessage(any());
    }

    @Test
    void unlikeCommentShouldNotDecreaseCountWhenAlreadyUnliked() {
        Comment comment = new Comment();
        comment.setCommentId(1);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(commentMapper.findById(1)).thenReturn(comment);
        when(commentLikeMapper.delete(1, 10L)).thenReturn(0);

        ApiResponse<EmptyVO> response = commentService.unlikeComment(1);

        assertEquals(200, response.getCode());
        verify(commentMapper, never()).decrementLikeCount(1);
    }
}
