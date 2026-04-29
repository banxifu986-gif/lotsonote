package com.banny.lotsonote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.banny.lotsonote.exception.ParamExceptionHandler;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.user.LoginRequest;
import com.banny.lotsonote.model.dto.user.RegisterRequest;
import com.banny.lotsonote.model.dto.user.SendVerifyCodeRequest;
import com.banny.lotsonote.model.dto.user.UserQueryParam;
import com.banny.lotsonote.model.vo.user.AdminUserVO;
import com.banny.lotsonote.model.vo.user.LoginUserVO;
import com.banny.lotsonote.model.vo.user.RegisterVO;
import com.banny.lotsonote.model.vo.user.UserVO;
import com.banny.lotsonote.service.EmailService;
import com.banny.lotsonote.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserController userController;

    @InjectMocks
    private EmailController emailController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController, emailController)
                .setControllerAdvice(new ParamExceptionHandler())
                .build();
    }

    @Test
    public void registerShouldPassResolvedClientIpToService() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setAccount("account01");
        request.setUsername("user01");
        request.setPassword("password01");
        request.setEmail("register@qq.com");
        request.setVerifyCode("123456");

        when(userService.register(any(RegisterRequest.class), eq("1.1.1.1")))
                .thenReturn(ApiResponse.success(new RegisterVO()));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(userService).register(captor.capture(), eq("1.1.1.1"));
        assertEquals("register@qq.com", captor.getValue().getEmail());
    }

    @Test
    public void loginShouldPassResolvedClientIpToService() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@qq.com");
        request.setVerifyCode("123456");

        when(userService.login(any(LoginRequest.class), eq("3.3.3.3")))
                .thenReturn(ApiResponse.success(new LoginUserVO()));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Real-IP", "3.3.3.3")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).login(any(LoginRequest.class), eq("3.3.3.3"));
    }

    @Test
    public void sendLoginVerifyCodeShouldReturnUnifiedSuccessMessageWhenEmailExists() throws Exception {
        SendVerifyCodeRequest request = new SendVerifyCodeRequest();
        request.setEmail("login@qq.com");
        request.setType("LOGIN");

        when(userService.existsByEmail("login@qq.com")).thenReturn(true);

        mockMvc.perform(post("/api/email/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "8.8.8.8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("如果该邮箱可用，验证码已发送"));

        verify(emailService).sendVerificationCode("login@qq.com", "8.8.8.8", com.banny.lotsonote.model.enums.user.VerifyCodeType.LOGIN, true);
    }

    @Test
    public void sendLoginVerifyCodeShouldFakeSuccessWhenEmailDoesNotExist() throws Exception {
        SendVerifyCodeRequest request = new SendVerifyCodeRequest();
        request.setEmail("missing@qq.com");
        request.setType("LOGIN");

        when(userService.existsByEmail("missing@qq.com")).thenReturn(false);

        mockMvc.perform(post("/api/email/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Real-IP", "9.9.9.9")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("如果该邮箱可用，验证码已发送"));

        verify(emailService).sendVerificationCode("missing@qq.com", "9.9.9.9", com.banny.lotsonote.model.enums.user.VerifyCodeType.LOGIN, false);
    }

    @Test
    public void sendVerifyCodeShouldRejectInvalidTypeAtValidationLayer() throws Exception {
        SendVerifyCodeRequest request = new SendVerifyCodeRequest();
        request.setEmail("login@qq.com");
        request.setType("UNKNOWN");

        mockMvc.perform(post("/api/email/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation Failed"));

        verify(emailService, never()).sendVerificationCode(any(), any(), any(), anyBoolean());
    }

    @Test
    public void getUserInfoShouldNotExposeEmail() throws Exception {
        UserVO userVO = new UserVO();
        userVO.setUsername("user01");
        userVO.setSchool("school01");

        when(userService.getUserInfo(100L)).thenReturn(ApiResponse.success(userVO));

        mockMvc.perform(get("/api/users/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("user01"))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    public void adminGetUserShouldNotExposePassword() throws Exception {
        AdminUserVO adminUserVO = new AdminUserVO();
        adminUserVO.setUserId(100L);
        adminUserVO.setAccount("account01");
        adminUserVO.setEmail("admin@qq.com");
        adminUserVO.setIsAdmin(1);

        when(userService.getUserList(any(UserQueryParam.class)))
                .thenReturn(ApiResponse.success(List.of(adminUserVO)));

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].account").value("account01"))
                .andExpect(jsonPath("$.data[0].email").value("admin@qq.com"))
                .andExpect(jsonPath("$.data[0].password").doesNotExist());
    }

    @Test
    public void uploadAvatarShouldReturnBadRequestWhenServiceRejectsInvalidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        when(userService.uploadAvatar(any())).thenThrow(new IllegalArgumentException("文件内容与图片格式不匹配"));

        mockMvc.perform(multipart("/api/users/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文件内容与图片格式不匹配"));
    }
}
