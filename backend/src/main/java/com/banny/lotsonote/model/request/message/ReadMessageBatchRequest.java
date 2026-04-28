package com.banny.lotsonote.model.request.message;

import lombok.Data;

import java.util.List;

@Data
public class ReadMessageBatchRequest {
    private List<Integer> messageIds;
}
