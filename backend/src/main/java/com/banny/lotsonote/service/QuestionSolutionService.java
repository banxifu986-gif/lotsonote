package com.banny.lotsonote.service;

import com.banny.lotsonote.model.entity.QuestionSolution;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface QuestionSolutionService {
    QuestionSolution findByQuestionId(Integer questionId);

    void saveOrUpdate(Integer questionId, String content);

    void deleteByQuestionId(Integer questionId);
}
