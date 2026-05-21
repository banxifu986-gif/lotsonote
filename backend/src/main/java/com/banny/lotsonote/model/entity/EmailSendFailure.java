package com.banny.lotsonote.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailSendFailure {
    private Long id;
    private String taskId;
    private String email;
    private String type;
    private Integer retryCount;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime failedAt;
    private String traceId;
    private Boolean expiredFlag;
}
