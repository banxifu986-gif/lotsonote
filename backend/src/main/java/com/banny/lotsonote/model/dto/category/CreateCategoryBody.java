package com.banny.lotsonote.model.dto.category;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateCategoryBody {

    @NotBlank(message = "name 不能为空")
    @NotNull(message = "name 不能为空")
    @Length(max = 32, min = 1, message = "name 长度在 1 - 32 之间")
    private String name;

    @NotNull(message = "parentCategoryId 不能为空")
    @Min(value = 0, message = "parentCategoryId 必须为正整数")
    private Integer parentCategoryId;
}
