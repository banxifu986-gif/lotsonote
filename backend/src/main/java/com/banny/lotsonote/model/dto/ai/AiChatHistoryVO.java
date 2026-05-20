package com.banny.lotsonote.model.dto.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatHistoryVO {
    private List<AiChatMessageVO> messages = new ArrayList<>();
}
