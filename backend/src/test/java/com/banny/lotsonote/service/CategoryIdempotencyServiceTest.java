package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.CategoryMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.category.CreateCategoryBody;
import com.banny.lotsonote.model.entity.Category;
import com.banny.lotsonote.model.vo.category.CreateCategoryVO;
import com.banny.lotsonote.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryIdempotencyServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private QuestionMapper questionMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl();
        ReflectionTestUtils.setField(categoryService, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(categoryService, "QuestionMapper", questionMapper);
    }

    @Test
    void createTopLevelCategoryShouldReturnExistingCategoryWhenRequestIsRepeated() {
        CreateCategoryBody body = new CreateCategoryBody();
        body.setName(" Java ");
        body.setParentCategoryId(0);

        Category existingCategory = new Category();
        existingCategory.setCategoryId(10);
        existingCategory.setName("Java");
        existingCategory.setParentCategoryId(0);

        when(categoryMapper.findByName("Java")).thenReturn(existingCategory);

        ApiResponse<CreateCategoryVO> response = categoryService.createCategory(body);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(10, response.getData().getCategoryId());
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void createChildCategoryShouldReturnExistingCategoryWhenRequestIsRepeated() {
        CreateCategoryBody body = new CreateCategoryBody();
        body.setName(" 数组 ");
        body.setParentCategoryId(5);

        Category parentCategory = new Category();
        parentCategory.setCategoryId(5);

        Category existingCategory = new Category();
        existingCategory.setCategoryId(12);
        existingCategory.setName("数组");
        existingCategory.setParentCategoryId(5);

        when(categoryMapper.findById(5)).thenReturn(parentCategory);
        when(categoryMapper.findByNameAndParentCategoryId("数组", 5)).thenReturn(existingCategory);

        ApiResponse<CreateCategoryVO> response = categoryService.createCategory(body);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(12, response.getData().getCategoryId());
        verify(categoryMapper, never()).insert(any(Category.class));
    }
}
