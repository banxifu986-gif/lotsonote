package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.annotation.NeedAdmin;
import com.banny.lotsonote.mapper.QuestionListItemMapper;
import com.banny.lotsonote.mapper.QuestionListMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.EmptyVO;
import com.banny.lotsonote.model.dto.questionList.CreateQuestionListBody;
import com.banny.lotsonote.model.dto.questionList.UpdateQuestionListBody;
import com.banny.lotsonote.model.entity.QuestionList;
import com.banny.lotsonote.model.vo.questionList.CreateQuestionListVO;
import com.banny.lotsonote.service.QuestionListService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionListServiceImpl implements QuestionListService {

    @Autowired
    private QuestionListMapper questionListMapper;

    @Autowired
    private QuestionListItemMapper questionListItemMapper;

    @Override
    @NeedAdmin
    public ApiResponse<QuestionList> getQuestionList(Integer questionListId) {
        return ApiResponseUtil.success("获取题单成功", questionListMapper.findById(questionListId));
    }

    @Override
    @NeedAdmin
    public ApiResponse<List<QuestionList>> getQuestionLists() {
        return ApiResponseUtil.success("获取题单成功", questionListMapper.findAll());
    }

    @Override
    @NeedAdmin
    public ApiResponse<CreateQuestionListVO> createQuestionList(CreateQuestionListBody body) {
        String questionListName = body.getName() == null ? null : body.getName().trim();
        QuestionList existingQuestionList = questionListMapper.findByNameAndType(questionListName, body.getType());
        if (existingQuestionList != null) {
            CreateQuestionListVO questionListVO = new CreateQuestionListVO();
            questionListVO.setQuestionListId(existingQuestionList.getQuestionListId());
            return ApiResponseUtil.success("题单已存在", questionListVO);
        }

        QuestionList questionList = new QuestionList();
        BeanUtils.copyProperties(body, questionList);
        questionList.setName(questionListName);

        // 创建题单
        try {
            questionListMapper.insert(questionList);
            CreateQuestionListVO questionListVO = new CreateQuestionListVO();
            questionListVO.setQuestionListId(questionList.getQuestionListId());
            return ApiResponseUtil.success("创建题单成功", questionListVO);
        } catch (Exception e) {
            return ApiResponseUtil.error("创建题单失败");
        }
    }

    @Override
    @NeedAdmin
    public ApiResponse<EmptyVO> deleteQuestionList(Integer questionListId) {
        // 删除题单，还需要删除题单对应的题单项目
        QuestionList questionList = questionListMapper.findById(questionListId);

        if (questionList == null) {
            return ApiResponseUtil.error("题单不存在");
        }

        try {
            questionListMapper.deleteById(questionListId);
            // 删除题单对应的所有题单项
            questionListItemMapper.deleteByQuestionListId(questionListId);
            return ApiResponseUtil.success("删除题单成功");
        } catch (Exception e) {
            return ApiResponseUtil.error("删除题单失败");
        }
    }

    @Override
    @NeedAdmin
    public ApiResponse<EmptyVO> updateQuestionList(Integer questionListId, UpdateQuestionListBody body) {

        QuestionList questionList = new QuestionList();
        BeanUtils.copyProperties(body, questionList);
        questionList.setQuestionListId(questionListId);

        System.out.println(questionList);

        try {
            questionListMapper.update(questionList);
            return ApiResponseUtil.success("更新题单成功");
        } catch (Exception e) {
            return ApiResponseUtil.error("更新题单失败");
        }
    }
}
