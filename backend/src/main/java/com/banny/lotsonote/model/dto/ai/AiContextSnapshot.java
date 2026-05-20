package com.banny.lotsonote.model.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiContextSnapshot {
    private String title;
    private String categoryName;
    private String contentPath;
    private String questionContent;
    private String examPoint;
    private String noteDraft;
    private String noteContent;
    private String referenceSolution;
    private String description;
    private List<String> relatedTitles;
}
