package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.entity.Note;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.service.impl.SearchServiceImpl;
import com.banny.lotsonote.utils.SearchUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SearchServiceImplTest {

    private SearchServiceImpl searchService;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        searchService = new SearchServiceImpl();
        ReflectionTestUtils.setField(searchService, "noteMapper", noteMapper);
        ReflectionTestUtils.setField(searchService, "userMapper", userMapper);
        ReflectionTestUtils.setField(searchService, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        response = new MockHttpServletResponse();
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void searchNotesShouldWriteMissHeadersWhenCacheMiss() {
        Note note = new Note();
        note.setNoteId(1);
        String keyword = "Java 面试";
        String processedKeyword = SearchUtils.preprocessKeyword(keyword);

        when(valueOperations.get("search:note:Java 面试:1:10")).thenReturn(null);
        when(noteMapper.searchNotes(eq(processedKeyword), eq(10), eq(0))).thenReturn(List.of(note));

        ApiResponse<List<Note>> responseBody = searchService.searchNotes(keyword, 1, 10);

        assertEquals(200, responseBody.getCode());
        assertEquals("MISS", response.getHeader("X-Search-Cache"));
        assertEquals("1", response.getHeader("X-Search-Result-Count"));
        assertNotNull(response.getHeader("X-Search-Duration-Ms"));
        verify(valueOperations).set(eq("search:note:Java 面试:1:10"), eq(List.of(note)), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    public void searchUsersShouldWriteHitHeadersWhenCacheHit() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("admin");

        when(valueOperations.get("search:user:admin:1:10")).thenReturn(List.of(user));

        ApiResponse<List<User>> responseBody = searchService.searchUsers("admin", 1, 10);

        assertEquals(200, responseBody.getCode());
        assertEquals("HIT", response.getHeader("X-Search-Cache"));
        assertEquals("1", response.getHeader("X-Search-Result-Count"));
        assertNotNull(response.getHeader("X-Search-Duration-Ms"));
        verify(userMapper, never()).searchUsers(anyString(), eq(10), eq(0));
    }
}
