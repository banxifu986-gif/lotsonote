package com.banny.lotsonote.exception;

/**
 * 业务异常，用于表示可预期的业务错误（如账号重复、用户不存在等）。
 * 由全局异常处理器捕获并转换为对应的错误响应，不需要在 Service 层手动 catch。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 默认使用 400 错误码 */
    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
