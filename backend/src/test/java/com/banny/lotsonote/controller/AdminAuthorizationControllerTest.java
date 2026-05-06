package com.banny.lotsonote.controller;

import com.banny.lotsonote.aspect.NeedAdminAspect;
import com.banny.lotsonote.exception.ParamExceptionHandler;
import com.banny.lotsonote.mapper.CategoryMapper;
import com.banny.lotsonote.mapper.QuestionMapper;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.dto.category.CreateCategoryBody;
import com.banny.lotsonote.model.dto.user.UserQueryParam;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.CategoryService;
import com.banny.lotsonote.service.UserService;
import com.banny.lotsonote.service.impl.CategoryServiceImpl;
import com.banny.lotsonote.service.impl.UserServiceImpl;
import com.banny.lotsonote.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminAuthorizationControllerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void adminUsersShouldReturnUnauthenticatedErrorWhenNotLoggedIn() throws Exception {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(false);
        MockMvc mockMvc = buildUserMockMvc(requestScopeData);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户未登录"));
    }

    @Test
    void adminUsersShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        User user = new User();
        user.setUserId(100L);
        user.setIsAdmin(0);
        when(userMapper.findById(100L)).thenReturn(user);
        MockMvc mockMvc = buildUserMockMvc(requestScopeData);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无管理员权限"));
    }

    @Test
    void adminUsersShouldSucceedWhenUserIsAdmin() throws Exception {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        User admin = new User();
        admin.setUserId(100L);
        admin.setIsAdmin(1);
        when(userMapper.findById(100L)).thenReturn(admin);

        User listedUser = new User();
        listedUser.setUserId(101L);
        listedUser.setAccount("account01");
        listedUser.setEmail("admin@qq.com");
        listedUser.setIsAdmin(1);

        UserQueryParam queryParam = new UserQueryParam();
        queryParam.setPage(1);
        queryParam.setPageSize(10);
        when(userMapper.countByQueryParam(any(UserQueryParam.class))).thenReturn(1);
        when(userMapper.findByQueryParam(any(UserQueryParam.class), org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(0)))
                .thenReturn(List.of(listedUser));
        MockMvc mockMvc = buildUserMockMvc(requestScopeData);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].account").value("account01"))
                .andExpect(jsonPath("$.data[0].email").value("admin@qq.com"));
    }

    @Test
    void adminCategoriesShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        User user = new User();
        user.setUserId(100L);
        user.setIsAdmin(0);
        when(userMapper.findById(100L)).thenReturn(user);
        MockMvc mockMvc = buildCategoryMockMvc(requestScopeData);

        CreateCategoryBody body = new CreateCategoryBody();
        body.setName("Java");
        body.setParentCategoryId(0);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无管理员权限"));
    }

    @Test
    void adminCategoriesShouldSucceedWhenUserIsAdmin() throws Exception {
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
        MockMvc mockMvc = buildCategoryMockMvc(requestScopeData);

        CreateCategoryBody body = new CreateCategoryBody();
        body.setName("Java");
        body.setParentCategoryId(0);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.categoryId").value(10));
    }

    private MockMvc buildUserMockMvc(RequestScopeData requestScopeData) {
        UserServiceImpl target = new UserServiceImpl();
        ReflectionTestUtils.setField(target, "userMapper", userMapper);
        ReflectionTestUtils.setField(target, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(target, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(target, "requestScopeData", requestScopeData);

        NeedAdminAspect aspect = new NeedAdminAspect();
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", userMapper);

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        UserService userService = factory.getProxy();

        UserController userController = new UserController();
        ReflectionTestUtils.setField(userController, "userService", userService);

        return MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new ParamExceptionHandler())
                .build();
    }

    private MockMvc buildCategoryMockMvc(RequestScopeData requestScopeData) {
        CategoryServiceImpl target = new CategoryServiceImpl();
        ReflectionTestUtils.setField(target, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(target, "QuestionMapper", questionMapper);

        NeedAdminAspect aspect = new NeedAdminAspect();
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", userMapper);

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        CategoryService categoryService = factory.getProxy();

        CategoryController categoryController = new CategoryController();
        ReflectionTestUtils.setField(categoryController, "categoryService", categoryService);

        return MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new ParamExceptionHandler())
                .build();
    }
}
