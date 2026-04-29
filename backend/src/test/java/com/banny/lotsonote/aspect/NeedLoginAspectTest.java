package com.banny.lotsonote.aspect;

import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.scope.RequestScopeData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NeedLoginAspectTest {

    @Test
    void aroundShouldReturnErrorWhenUserNotLoggedIn() throws Throwable {
        NeedLoginAspect aspect = new NeedLoginAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(false);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.around(joinPoint, null);

        ApiResponse<?> response = (ApiResponse<?>) result;
        assertEquals(400, response.getCode());
        assertEquals("用户未登录", response.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void aroundShouldProceedWhenUserLoggedIn() throws Throwable {
        NeedLoginAspect aspect = new NeedLoginAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, null);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }
}
