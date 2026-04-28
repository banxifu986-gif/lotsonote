package com.banny.lotsonote.service;

import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.TokenApiResponse;
import com.banny.lotsonote.model.dto.user.LoginRequest;
import com.banny.lotsonote.model.dto.user.RegisterRequest;
import com.banny.lotsonote.model.dto.user.UserQueryParam;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.model.enums.user.VerifyCodeType;
import com.banny.lotsonote.model.vo.user.AdminUserVO;
import com.banny.lotsonote.model.vo.user.LoginUserVO;
import com.banny.lotsonote.model.vo.user.RegisterVO;
import com.banny.lotsonote.model.vo.user.UserVO;
import com.banny.lotsonote.service.impl.UserServiceImpl;
import com.banny.lotsonote.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @BeforeEach
    public void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(userService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(userService, "emailService", emailService);
    }

    @Test
    public void registerShouldVerifyRegisterCodeWithRealIp() {
        RegisterRequest request = new RegisterRequest();
        request.setAccount("account01");
        request.setUsername("user01");
        request.setPassword("password01");
        request.setEmail("register@qq.com");
        request.setVerifyCode("123456");

        when(userMapper.findByAccount(request.getAccount())).thenReturn(null);
        when(userMapper.findByEmail(request.getEmail())).thenReturn(null);
        when(emailService.checkVerificationCode(request.getEmail(), request.getVerifyCode(), "127.0.0.1", VerifyCodeType.REGISTER))
                .thenReturn(true);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100L);
            return null;
        }).when(userMapper).insert(any(User.class));
        when(jwtUtil.generateToken(100L)).thenReturn("register-token");

        ApiResponse<RegisterVO> response = userService.register(request, "127.0.0.1");

        assertInstanceOf(TokenApiResponse.class, response);
        assertEquals(200, response.getCode());
        verify(emailService).checkVerificationCode(request.getEmail(), request.getVerifyCode(), "127.0.0.1", VerifyCodeType.REGISTER);
        verify(userMapper).insert(any(User.class));
        verify(userMapper).updateLastLoginAt(100L);
    }

    @Test
    public void registerShouldRejectWhenRegisterCodeInvalid() {
        RegisterRequest request = new RegisterRequest();
        request.setAccount("account01");
        request.setUsername("user01");
        request.setPassword("password01");
        request.setEmail("register@qq.com");
        request.setVerifyCode("123456");

        when(userMapper.findByAccount(request.getAccount())).thenReturn(null);
        when(userMapper.findByEmail(request.getEmail())).thenReturn(null);
        when(emailService.checkVerificationCode(request.getEmail(), request.getVerifyCode(), "127.0.0.1", VerifyCodeType.REGISTER))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(request, "127.0.0.1"));

        assertEquals("验证码无效或已过期", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    public void loginShouldUseVerifyCodeBranchAndReturnToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@qq.com");
        request.setVerifyCode("123456");
        request.setAccount("ignored01");

        User user = new User();
        user.setUserId(200L);
        user.setEmail("login@qq.com");
        user.setAccount("account01");
        user.setUsername("user01");

        when(userMapper.findByEmail("login@qq.com")).thenReturn(user);
        when(emailService.checkVerificationCode("login@qq.com", "123456", "10.0.0.1", VerifyCodeType.LOGIN))
                .thenReturn(true);
        when(jwtUtil.generateToken(200L)).thenReturn("login-token");

        ApiResponse<LoginUserVO> response = userService.login(request, "10.0.0.1");

        assertInstanceOf(TokenApiResponse.class, response);
        assertEquals(200, response.getCode());
        assertEquals("login@qq.com", response.getData().getEmail());
        verify(userMapper, never()).findByAccount(any());
        verify(emailService).checkVerificationCode("login@qq.com", "123456", "10.0.0.1", VerifyCodeType.LOGIN);
        verify(userMapper).updateLastLoginAt(200L);
    }

    @Test
    public void loginShouldReturnUnifiedErrorWhenEmailNotFoundForVerifyCodeLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@qq.com");
        request.setVerifyCode("123456");

        when(userMapper.findByEmail("missing@qq.com")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(request, "10.0.0.1"));

        assertEquals("验证码无效或已过期", exception.getMessage());
        verify(emailService, never()).checkVerificationCode(eq("missing@qq.com"), eq("123456"), eq("10.0.0.1"), eq(VerifyCodeType.LOGIN));
    }

    @Test
    public void loginShouldKeepPasswordLoginWorking() {
        LoginRequest request = new LoginRequest();
        request.setAccount("account01");
        request.setPassword("password01");

        User user = new User();
        user.setUserId(300L);
        user.setAccount("account01");
        user.setPassword("encoded-password");
        user.setUsername("user01");

        when(userMapper.findByAccount("account01")).thenReturn(user);
        when(passwordEncoder.matches("password01", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(300L)).thenReturn("password-token");

        ApiResponse<LoginUserVO> response = userService.login(request, "10.0.0.1");

        assertInstanceOf(TokenApiResponse.class, response);
        assertEquals(200, response.getCode());
        verify(emailService, never()).checkVerificationCode(any(), any(), any(), any());
        verify(userMapper).updateLastLoginAt(300L);
    }

    @Test
    public void getUserInfoShouldNotReturnEmail() {
        User user = new User();
        user.setUserId(100L);
        user.setUsername("user01");
        user.setEmail("user01@qq.com");
        user.setSchool("school01");

        when(userMapper.findById(100L)).thenReturn(user);

        ApiResponse<UserVO> response = userService.getUserInfo(100L);

        assertEquals(200, response.getCode());
        assertEquals("user01", response.getData().getUsername());
        assertEquals("school01", response.getData().getSchool());
        assertThrows(NoSuchMethodException.class, () -> UserVO.class.getMethod("getEmail"));
    }

    @Test
    public void getUserListShouldConvertToAdminUserVOWithoutPassword() {
        UserQueryParam queryParam = new UserQueryParam();
        queryParam.setPage(1);
        queryParam.setPageSize(10);

        User user = new User();
        user.setUserId(100L);
        user.setAccount("account01");
        user.setPassword("encoded-password");
        user.setEmail("admin@qq.com");
        user.setIsAdmin(1);

        when(userMapper.countByQueryParam(queryParam)).thenReturn(1);
        when(userMapper.findByQueryParam(queryParam, 10, 0)).thenReturn(List.of(user));

        ApiResponse<List<AdminUserVO>> response = userService.getUserList(queryParam);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals("account01", response.getData().get(0).getAccount());
        assertEquals("admin@qq.com", response.getData().get(0).getEmail());
        assertEquals(1, response.getData().get(0).getIsAdmin());
    }
}
