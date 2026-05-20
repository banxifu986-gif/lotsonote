package com.banny.lotsonote.mapper;

import com.banny.lotsonote.model.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatSessionMapper {
    List<AiChatSession> findByUserId(@Param("userId") Long userId);

    AiChatSession findById(@Param("sessionId") Long sessionId);

    AiChatSession findLatestByUserIdAndBiz(@Param("userId") Long userId,
                                           @Param("sceneType") String sceneType,
                                           @Param("bizType") String bizType,
                                           @Param("bizId") String bizId);

    int insert(AiChatSession session);

    int updateTitleAndTouch(@Param("sessionId") Long sessionId, @Param("title") String title);

    int updateContextAndTouch(@Param("sessionId") Long sessionId,
                              @Param("title") String title,
                              @Param("contextSnapshot") String contextSnapshot);

    int touch(@Param("sessionId") Long sessionId);
}
