package com.banny.lotsonote.model.dto.user;

import com.banny.lotsonote.utils.LogSanitizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendVerifyCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "REGISTER|RESET_PASSWORD|LOGIN", message = "验证码类型不支持")
    private String type;

    @Override
    public String toString() {
        return "SendVerifyCodeRequest(email=" + LogSanitizer.maskEmail(email)
                + ", type=" + type
                + ")";
    }
}
