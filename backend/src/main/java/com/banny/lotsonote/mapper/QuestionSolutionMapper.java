package com.banny.lotsonote.mapper;

import com.banny.lotsonote.model.entity.QuestionSolution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionSolutionMapper {
    QuestionSolution findByQuestionId(@Param("questionId") Integer questionId);

    int insert(QuestionSolution questionSolution);

    int updateByQuestionId(QuestionSolution questionSolution);

    int deleteByQuestionId(@Param("questionId") Integer questionId);
}
