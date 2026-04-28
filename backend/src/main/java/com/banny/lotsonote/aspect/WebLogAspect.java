package com.banny.lotsonote.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.banny.lotsonote.utils.LogSanitizer;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    @Around("execution(public * com.banny.lotsonote.controller..*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.info("请求开始 url={} method={} ip={} class_method={} args={}",
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getRemoteAddr(),
                    signature.getDeclaringTypeName() + "." + signature.getName(),
                    sanitizeArgs(signature, joinPoint.getArgs()));
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("请求结束 耗时={}ms result={}", duration, sanitizeResult(result));
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("请求异常 耗时={}ms error={}", duration, e.getMessage());
            throw e;
        }
    }

    String sanitizeArgs(MethodSignature signature, Object[] args) {
        return LogSanitizer.sanitizeArguments(signature.getParameterNames(), args);
    }

    String sanitizeResult(Object result) {
        return LogSanitizer.sanitize(result);
    }
}
