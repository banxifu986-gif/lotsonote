package com.banny.lotsonote.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NoteRankMetricsFilterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private FilterChain filterChain;

    @Test
    public void shouldWriteHeadersForRankRequest() throws ServletException, IOException {
        String rankKey = "rank:note:daily:" + LocalDate.now();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes/ranklist");
        MockHttpServletResponse response = new MockHttpServletResponse();
        NoteRankMetricsFilter filter = new NoteRankMetricsFilter(redisTemplate);

        when(redisTemplate.hasKey(rankKey)).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(rankKey, 0, 9)).thenReturn(Set.of("1", "2"));

        filter.doFilter(request, response, filterChain);

        assertEquals("HIT", response.getHeader("X-Rank-Cache"));
        assertEquals("2", response.getHeader("X-Rank-Result-Count"));
    }
}
