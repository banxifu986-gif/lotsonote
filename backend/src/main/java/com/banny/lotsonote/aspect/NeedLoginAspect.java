package com.banny.lotsonote.aspect;

import com.banny.lotsonote.annotation.NeedLogin;
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
public class NeedLoginAspect {

    @Autowired
    private RequestScopeData requestScopeData;

    @Around("@annotation(needLogin)")
    public Object around(ProceedingJoinPoint joinPoint, NeedLogin needLogin) throws Throwable {
        if (!requestScopeData.isLogin()) {
            return ApiResponseUtil.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        if (requestScopeData.getUserId() == null) {
            return ApiResponseUtil.error(HttpStatus.UNAUTHORIZED.value(), "用户登录状态异常");
        }

        return joinPoint.proceed();
    }
}
