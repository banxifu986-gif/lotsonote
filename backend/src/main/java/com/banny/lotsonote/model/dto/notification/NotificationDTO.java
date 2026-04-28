package com.banny.lotsonote.model.dto.notification;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class NotificationDTO {
    @NotEmpty(message = "content 不能为空")
    @NotNull(message = "content 不能为空")
    private String content;
}
