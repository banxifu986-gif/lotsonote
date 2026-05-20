package com.banny.lotsonote.aspect;

import com.banny.lotsonote.annotation.NeedAdmin;
import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.utils.ApiResponseUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class NeedAdminAspect {

    @Autowired
    private RequestScopeData requestScopeData;

    @Autowired
    private UserMapper userMapper;

    @Around("@annotation(needAdmin)")
    public Object around(ProceedingJoinPoint joinPoint, NeedAdmin needAdmin) throws Throwable {
        if (!requestScopeData.isLogin()) {
            return ApiResponseUtil.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        Long userId = requestScopeData.getUserId();
        if (userId == null) {
            return ApiResponseUtil.error(HttpStatus.UNAUTHORIZED.value(), "用户登录状态异常");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponseUtil.error(HttpStatus.UNAUTHORIZED.value(), "用户不存在");
        }

        if (!Integer.valueOf(1).equals(user.getIsAdmin())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), "无管理员权限");
        }

        return joinPoint.proceed();
    }
}
