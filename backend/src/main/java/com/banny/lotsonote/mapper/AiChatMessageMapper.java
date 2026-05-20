package com.banny.lotsonote.mapper;

import com.banny.lotsonote.model.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatMessageMapper {
    List<AiChatMessage> findBySessionId(@Param("sessionId") Long sessionId);

    int insert(AiChatMessage message);
}
