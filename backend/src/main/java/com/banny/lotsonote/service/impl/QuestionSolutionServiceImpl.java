package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.mapper.QuestionSolutionMapper;
import com.banny.lotsonote.model.entity.QuestionSolution;
import com.banny.lotsonote.service.QuestionSolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QuestionSolutionServiceImpl implements QuestionSolutionService {

    @Autowired
    private QuestionSolutionMapper questionSolutionMapper;

    @Override
    public QuestionSolution findByQuestionId(Integer questionId) {
        return questionSolutionMapper.findByQuestionId(questionId);
    }

    @Override
    public void saveOrUpdate(Integer questionId, String content) {
        String normalizedContent = normalizeContent(content);
        QuestionSolution existingSolution = questionSolutionMapper.findByQuestionId(questionId);

        if (!StringUtils.hasText(normalizedContent)) {
            if (existingSolution != null) {
                questionSolutionMapper.deleteByQuestionId(questionId);
            }
            return;
        }

        if (existingSolution == null) {
            QuestionSolution questionSolution = new QuestionSolution();
            questionSolution.setQuestionId(questionId);
            questionSolution.setContent(normalizedContent);
            questionSolutionMapper.insert(questionSolution);
            return;
        }

        existingSolution.setContent(normalizedContent);
        questionSolutionMapper.updateByQuestionId(existingSolution);
    }

    @Override
    public void deleteByQuestionId(Integer questionId) {
        questionSolutionMapper.deleteByQuestionId(questionId);
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        return content.trim();
    }
}
