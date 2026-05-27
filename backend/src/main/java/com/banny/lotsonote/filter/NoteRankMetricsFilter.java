package com.banny.lotsonote.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NoteRankMetricsFilter implements Filter {

    private static final String RANK_PATH = "/api/notes/ranklist";
    private static final String RANK_CACHE_HEADER = "X-Rank-Cache";
    private static final String RANK_DURATION_HEADER = "X-Rank-Duration-Ms";
    private static final String RANK_RESULT_COUNT_HEADER = "X-Rank-Result-Count";

    private final RedisTemplate<String, Object> redisTemplate;

    public NoteRankMetricsFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        if (!RANK_PATH.equals(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String rankKey = "rank:note:daily:" + LocalDate.now();
        boolean cacheHitBefore = Boolean.TRUE.equals(redisTemplate.hasKey(rankKey));
        long startTime = System.nanoTime();

        chain.doFilter(request, response);

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(rankKey, 0, 9);

        httpResponse.setHeader(RANK_CACHE_HEADER, cacheHitBefore ? "HIT" : "MISS");
        httpResponse.setHeader(RANK_DURATION_HEADER, String.valueOf(durationMs));
        httpResponse.setHeader(RANK_RESULT_COUNT_HEADER, String.valueOf(members == null ? 0 : members.size()));
    }
}
