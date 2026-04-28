package com.banny.lotsonote.controller;

import com.banny.lotsonote.model.enums.user.VerifyCodeType;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.user.SendVerifyCodeRequest;
import com.banny.lotsonote.service.UserService;
import com.banny.lotsonote.service.EmailService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @PostMapping("/verify-code")
    public ApiResponse<Void> sendVerifyCode(@Valid @RequestBody SendVerifyCodeRequest requestBody,
                                            HttpServletRequest request) {
        VerifyCodeType type = VerifyCodeType.valueOf(requestBody.getType());
        String clientIp = getClientIp(request);
        boolean shouldSend = shouldSendCode(requestBody.getEmail(), type);
        emailService.sendVerificationCode(requestBody.getEmail(), clientIp, type, shouldSend);
        if (type == VerifyCodeType.LOGIN) {
            return new ApiResponse<>(200, "如果该邮箱可用，验证码已发送", null);
        }
        return ApiResponseUtil.success(null);
    }

    private boolean shouldSendCode(String email, VerifyCodeType type) {
        if (type != VerifyCodeType.LOGIN) {
            return true;
        }
        return userService.existsByEmail(email);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
