package com.banny.lotsonote.aspect;

import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.scope.RequestScopeData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NeedAdminAspectTest {

    @Test
    void aroundShouldReturnErrorWhenUserNotLoggedIn() throws Throwable {
        NeedAdminAspect aspect = new NeedAdminAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(false);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", mock(UserMapper.class));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.around(joinPoint, null);

        ApiResponse<?> response = (ApiResponse<?>) result;
        assertEquals(400, response.getCode());
        assertEquals("用户未登录", response.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void aroundShouldReturnErrorWhenUserIdMissing() throws Throwable {
        NeedAdminAspect aspect = new NeedAdminAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(null);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", mock(UserMapper.class));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.around(joinPoint, null);

        ApiResponse<?> response = (ApiResponse<?>) result;
        assertEquals(400, response.getCode());
        assertEquals("用户 ID 异常", response.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void aroundShouldReturnErrorWhenUserNotFound() throws Throwable {
        NeedAdminAspect aspect = new NeedAdminAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.findById(100L)).thenReturn(null);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", userMapper);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.around(joinPoint, null);

        ApiResponse<?> response = (ApiResponse<?>) result;
        assertEquals(400, response.getCode());
        assertEquals("用户不存在", response.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void aroundShouldThrowBusinessExceptionWhenUserIsNotAdmin() {
        NeedAdminAspect aspect = new NeedAdminAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setUserId(100L);
        user.setIsAdmin(0);
        when(userMapper.findById(100L)).thenReturn(user);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", userMapper);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        BusinessException exception = assertThrows(BusinessException.class, () -> aspect.around(joinPoint, null));

        assertEquals(403, exception.getCode());
        assertEquals("无管理员权限", exception.getMessage());
    }

    @Test
    void aroundShouldProceedWhenUserIsAdmin() throws Throwable {
        NeedAdminAspect aspect = new NeedAdminAspect();
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setUserId(100L);
        user.setIsAdmin(1);
        when(userMapper.findById(100L)).thenReturn(user);
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(aspect, "userMapper", userMapper);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, null);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }
}
