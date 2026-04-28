package com.banny.lotsonote.aspect;

import com.banny.lotsonote.model.base.TokenApiResponse;
import com.banny.lotsonote.model.dto.user.LoginRequest;
import com.banny.lotsonote.model.dto.user.RegisterRequest;
import com.banny.lotsonote.model.dto.user.SendVerifyCodeRequest;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebLogAspectTest {

    @Test
    void shouldMaskSensitiveFieldsInRequestAndResponseLogs() {
        WebLogAspect aspect = new WebLogAspect();
        MethodSignature signature = mock(MethodSignature.class);

        LoginRequest request = new LoginRequest();
        request.setAccount("account01");
        request.setEmail("login@qq.com");
        request.setPassword("password01");
        request.setVerifyCode("123456");

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

        TokenApiResponse<Map<String, Object>> response = new TokenApiResponse<>(
                200,
                "登录成功",
                Map.of("email", "result@qq.com", "phone", "13812345678"),
                "secret-token"
        );

        when(signature.getParameterNames()).thenReturn(new String[]{"request", "httpServletRequest"});

        String sanitizedArgs = aspect.sanitizeArgs(signature, new Object[]{request, httpServletRequest});
        String sanitizedResult = aspect.sanitizeResult(response);

        assertTrue(sanitizedArgs.contains("l***n@qq.com"));
        assertTrue(sanitizedResult.contains("r***t@qq.com"));
        assertTrue(sanitizedResult.contains("138****5678"));
        assertFalse(sanitizedArgs.contains("password01"));
        assertFalse(sanitizedArgs.contains("123456"));
        assertFalse(sanitizedResult.contains("secret-token"));
        assertFalse(sanitizedArgs.contains("\"password\""));
        assertFalse(sanitizedArgs.contains("\"verifyCode\""));
        assertFalse(sanitizedResult.contains("\"token\""));
    }

    @Test
    void dtoAndResponseToStringShouldAlsoMaskSensitiveFields() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setAccount("account01");
        registerRequest.setUsername("user01");
        registerRequest.setPassword("password01");
        registerRequest.setEmail("register@qq.com");
        registerRequest.setVerifyCode("123456");

        SendVerifyCodeRequest sendVerifyCodeRequest = new SendVerifyCodeRequest();
        sendVerifyCodeRequest.setEmail("verify@qq.com");
        sendVerifyCodeRequest.setType("LOGIN");

        TokenApiResponse<Map<String, Object>> response = new TokenApiResponse<>(
                200,
                "success",
                Map.of("email", "result@qq.com"),
                "secret-token"
        );

        assertTrue(registerRequest.toString().contains("r***r@qq.com"));
        assertFalse(registerRequest.toString().contains("password01"));
        assertFalse(registerRequest.toString().contains("123456"));

        assertTrue(sendVerifyCodeRequest.toString().contains("v***y@qq.com"));
        assertFalse(sendVerifyCodeRequest.toString().contains("verify@qq.com"));

        assertTrue(response.toString().contains("r***t@qq.com"));
        assertFalse(response.toString().contains("secret-token"));
    }
}
