package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.entity.Note;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.service.SearchService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import com.banny.lotsonote.utils.SearchUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class SearchServiceImpl implements SearchService {

    private static final String NOTE_SEARCH_CACHE_KEY = "search:note:%s:%d:%d";
    private static final String USER_SEARCH_CACHE_KEY = "search:user:%s:%d:%d";
    private static final String NOTE_TAG_SEARCH_CACHE_KEY = "search:note:tag:%s:%s:%d:%d";
    private static final String SEARCH_CACHE_HEADER = "X-Search-Cache";
    private static final String SEARCH_DURATION_HEADER = "X-Search-Duration-Ms";
    private static final String SEARCH_RESULT_COUNT_HEADER = "X-Search-Result-Count";
    private static final long CACHE_EXPIRE_TIME = 30;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public ApiResponse<List<Note>> searchNotes(String keyword, int page, int pageSize) {
        long startTime = System.nanoTime();
        boolean cacheHit = false;
        int resultCount = 0;
        try {
            String cacheKey = String.format(NOTE_SEARCH_CACHE_KEY, keyword, page, pageSize);
            List<Note> cachedResult = (List<Note>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                cacheHit = true;
                resultCount = cachedResult.size();
                return ApiResponseUtil.success("搜索成功", cachedResult);
            }

            keyword = SearchUtils.preprocessKeyword(keyword);
            int offset = SearchUtils.calculateOffset(page, pageSize);
            List<Note> notes = noteMapper.searchNotes(keyword, pageSize, offset);
            resultCount = notes.size();
            redisTemplate.opsForValue().set(cacheKey, notes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            return ApiResponseUtil.success("搜索成功", notes);
        } catch (Exception e) {
            log.error("搜索笔记失败", e);
            return ApiResponseUtil.error("搜索失败");
        } finally {
            writeSearchMetrics(cacheHit, resultCount, startTime);
        }
    }

    @Override
    public ApiResponse<List<User>> searchUsers(String keyword, int page, int pageSize) {
        long startTime = System.nanoTime();
        boolean cacheHit = false;
        int resultCount = 0;
        try {
            String cacheKey = String.format(USER_SEARCH_CACHE_KEY, keyword, page, pageSize);
            List<User> cachedResult = (List<User>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                cacheHit = true;
                resultCount = cachedResult.size();
                return ApiResponseUtil.success("搜索成功", cachedResult);
            }

            int offset = SearchUtils.calculateOffset(page, pageSize);
            List<User> users = userMapper.searchUsers(keyword, pageSize, offset);
            resultCount = users.size();
            redisTemplate.opsForValue().set(cacheKey, users, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            return ApiResponseUtil.success("搜索成功", users);
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return ApiResponseUtil.error("搜索失败");
        } finally {
            writeSearchMetrics(cacheHit, resultCount, startTime);
        }
    }

    @Override
    public ApiResponse<List<Note>> searchNotesByTag(String keyword, String tag, int page, int pageSize) {
        long startTime = System.nanoTime();
        boolean cacheHit = false;
        int resultCount = 0;
        try {
            String cacheKey = String.format(NOTE_TAG_SEARCH_CACHE_KEY, keyword, tag, page, pageSize);
            List<Note> cachedResult = (List<Note>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                cacheHit = true;
                resultCount = cachedResult.size();
                return ApiResponseUtil.success("搜索成功", cachedResult);
            }

            keyword = SearchUtils.preprocessKeyword(keyword);
            int offset = SearchUtils.calculateOffset(page, pageSize);
            List<Note> notes = noteMapper.searchNotesByTag(keyword, tag, pageSize, offset);
            resultCount = notes.size();
            redisTemplate.opsForValue().set(cacheKey, notes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            return ApiResponseUtil.success("搜索成功", notes);
        } catch (Exception e) {
            log.error("搜索标签笔记失败", e);
            return ApiResponseUtil.error("搜索失败");
        } finally {
            writeSearchMetrics(cacheHit, resultCount, startTime);
        }
    }

    private void writeSearchMetrics(boolean cacheHit, int resultCount, long startTime) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return;
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        response.setHeader(SEARCH_CACHE_HEADER, cacheHit ? "HIT" : "MISS");
        response.setHeader(SEARCH_DURATION_HEADER, String.valueOf(durationMs));
        response.setHeader(SEARCH_RESULT_COUNT_HEADER, String.valueOf(resultCount));
    }
}
