package com.banny.lotsonote.service;

import com.banny.lotsonote.aspect.NeedAdminAspect;
import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.mapper.CategoryMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.category.CreateCategoryBody;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.model.vo.category.CreateCategoryVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryAdminAuthorizationTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private UserMapper userMapper;

    @Test
    void createCategoryShouldRejectNormalUser() {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        User user = new User();
        user.setUserId(100L);
        user.setIsAdmin(0);
        when(userMapper.findById(100L)).thenReturn(user);

        CategoryService categoryService = createProxy(requestScopeData);
        CreateCategoryBody body = new CreateCategoryBody();
        body.setName("Java");
        body.setParentCategoryId(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> categoryService.createCategory(body));

        assertEquals(403, exception.getCode());
        assertEquals("无管理员权限", exception.getMessage());
        verify(categoryMapper, never()).insert(any());
    }

    @Test
    void createCategoryShouldAllowAdminUser() {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        User user = new User();
        user.setUserId(100L);
        user.setIsAdmin(1);
        when(userMapper.findById(100L)).thenReturn(user);
        when(categoryMapper.findByName("Java")).thenReturn(null);
        doAnswer(invocation -> {
            com.banny.lotsonote.model.entity.Category category = invocation.getArgument(0);
            category.setCategoryId(10);
            return 1;
        }).when(categoryMapper).insert(any());

        CategoryService categoryService = createProxy(requestScopeData);
        CreateCategoryBody body = new CreateCategoryBody();
        body.setName("Java");
        body.setParentCategoryId(0);

        ApiResponse<CreateCategoryVO> response = categoryService.createCategory(body);

        assertEquals(200, response.getCode());
        assertEquals(10, response.getData().getCategoryId());
        verify(categoryMapper).insert(any());
    }

    private CategoryService createProxy(RequestScopeData requestScopeData) {
        CategoryServiceImpl target = new CategoryServiceImpl();
        ReflectionTestUtils.setField(target, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(target, "QuestionMapper", questionMapper);

        NeedAdminAspect aspect = new NeedAdminAspect();
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", userMapper);

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }
}
