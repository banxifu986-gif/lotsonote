package com.banny.lotsonote.task.email;

import lombok.Data;

@Data
public class EmailTask {
    private String taskId;
    private String email;
    private String code;
    private String type;
    private int retryCount;
    private long createdAt;
    private long expireAt;
    private String traceId;
    private String requestIp;
    private String failureReason;
}
