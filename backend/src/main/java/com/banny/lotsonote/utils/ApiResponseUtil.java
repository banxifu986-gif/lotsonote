package com.banny.lotsonote.utils;

import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.Pagination;
import com.banny.lotsonote.model.base.PaginationApiResponse;
import com.banny.lotsonote.model.base.TokenApiResponse;
import org.springframework.http.HttpStatus;

public class ApiResponseUtil {

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.success(null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.success(data);
    }

    public static <T> ApiResponse<T> error(String msg) {
        return error(HttpStatus.BAD_REQUEST.value(), msg);
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return ApiResponse.error(code, msg);
    }

    public static <T> TokenApiResponse<T> success(String msg, T data, String token) {
        return new TokenApiResponse<>(HttpStatus.OK.value(), msg, data, token);
    }

    public static <T> PaginationApiResponse<T> success(String msg, T data, Pagination pagination) {
        return new PaginationApiResponse<>(HttpStatus.OK.value(), msg, data, pagination);
    }
}
