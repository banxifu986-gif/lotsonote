package com.banny.lotsonote.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionSolution {
    private Integer solutionId;
    private Integer questionId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
