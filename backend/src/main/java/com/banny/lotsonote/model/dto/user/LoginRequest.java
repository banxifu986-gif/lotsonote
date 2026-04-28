package com.banny.lotsonote.model.dto.user;

import com.banny.lotsonote.utils.LogSanitizer;
import lombok.Data;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    /*
     * 用户账号
     */
    @Size(min = 6, max = 32, message = "账号长度必须在 6 到 32 个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String account;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 个字符之间")
    private String password;

    @Size(min = 6, max = 6, message = "验证码长度必须为6位")
    private String verifyCode;

    @AssertTrue(message = "账号和邮箱必须至少提供一个")
    private boolean isValidLogin() {
        return account != null || email != null;
    }

    @AssertTrue(message = "密码和验证码必须提供其中一个")
    private boolean isValidCredential() {
        return isNotBlank(password) || isNotBlank(verifyCode);
    }

    @AssertTrue(message = "验证码登录必须提供邮箱")
    private boolean isEmailPresentWhenUsingVerifyCode() {
        return !isNotBlank(verifyCode) || isNotBlank(email);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "LoginRequest(account=" + account
                + ", email=" + LogSanitizer.maskEmail(email)
                + ")";
    }
}
