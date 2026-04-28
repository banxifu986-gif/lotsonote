package com.banny.lotsonote.model.dto.message;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 消息查询参数
 */
@Data
public class MessageQueryParams {
    /**
     * 消息类型
     */
    private String type;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 当前页码，从1开始
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer pageSize = 10;

    /**
     * 排序字段，只允许白名单内的列名，防止 SQL 注入
     */
    @Pattern(regexp = "^(created_at|updated_at|type)$", message = "排序字段不合法")
    private String sortField = "created_at";

    /**
     * 排序方向，只允许 asc 或 desc
     */
    @Pattern(regexp = "^(asc|desc)$", message = "排序方向只能为 asc 或 desc")
    private String sortOrder = "desc";
} 