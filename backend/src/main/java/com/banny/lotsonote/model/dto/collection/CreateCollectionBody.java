package com.banny.lotsonote.model.dto.collection;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateCollectionBody {
    @NotNull(message = "name 不能为空")
    @NotBlank(message = "name 不能为空")
    private String name;
    private String description;
}
