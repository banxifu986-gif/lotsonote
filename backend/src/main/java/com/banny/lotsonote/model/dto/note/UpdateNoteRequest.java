package com.banny.lotsonote.model.dto.note;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 更新笔记请求DTO
 */
@Data
public class UpdateNoteRequest {
    /*
     * 笔记内容
     */
    @NotNull(message = "笔记内容不能为空")
    @NotBlank(message = "笔记内容不能为空")
    private String content;
} 