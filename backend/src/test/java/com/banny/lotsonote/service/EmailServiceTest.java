package com.banny.lotsonote.service;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.model.enums.redisKey.RedisKey;
import com.banny.lotsonote.model.enums.user.VerifyCodeType;
import com.banny.lotsonote.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    private EmailServiceImpl emailService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    public void setUp() {
        emailService = new EmailServiceImpl();
        ReflectionTestUtils.setField(emailService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(emailService, "rabbitTemplate", rabbitTemplate);
        ReflectionTestUtils.setField(emailService, "limitExpireSeconds", 60);
        ReflectionTestUtils.setField(emailService, "maxErrorCount", 5);
        ReflectionTestUtils.setField(emailService, "errorExpireMinutes", 5);
        ReflectionTestUtils.setField(emailService, "emailIpLimitCount", 5);
        ReflectionTestUtils.setField(emailService, "emailIpLimitExpireMinutes", 10);
        ReflectionTestUtils.setField(emailService, "ipLimitCount", 20);
        ReflectionTestUtils.setField(emailService, "ipLimitExpireMinutes", 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void checkVerificationCodeShouldClearCodeAndErrorCountWhenCodeIsCorrect() {
        String email = "example@qq.com";
        String ip = "127.0.0.1";
        String code = "123456";
        String codeKey = RedisKey.verificationCode(VerifyCodeType.LOGIN.name(), email);
        String errorCountKey = RedisKey.verificationErrorCount(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);
        String emailIpLimitKey = RedisKey.verificationEmailIpRateLimit(VerifyCodeType.LOGIN.name(), email, ip);

        when(valueOperations.get(errorCountKey)).thenReturn(null);
        when(valueOperations.get(ipLimitKey)).thenReturn(null);
        when(valueOperations.get(emailIpLimitKey)).thenReturn(null);
        when(valueOperations.get(codeKey)).thenReturn(code);

        assertTrue(emailService.checkVerificationCode(email, code, ip, VerifyCodeType.LOGIN));
        verify(redisTemplate).delete(codeKey);
        verify(redisTemplate).delete(errorCountKey);
    }

    @Test
    public void checkVerificationCodeShouldRecordFailureWhenCodeIsWrong() {
        String email = "example@qq.com";
        String ip = "127.0.0.1";
        String codeKey = RedisKey.verificationCode(VerifyCodeType.LOGIN.name(), email);
        String errorCountKey = RedisKey.verificationErrorCount(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);
        String emailIpLimitKey = RedisKey.verificationEmailIpRateLimit(VerifyCodeType.LOGIN.name(), email, ip);

        when(valueOperations.get(errorCountKey)).thenReturn(null);
        when(valueOperations.get(ipLimitKey)).thenReturn(null);
        when(valueOperations.get(emailIpLimitKey)).thenReturn(null);
        when(valueOperations.get(codeKey)).thenReturn("123456");
        when(valueOperations.increment(errorCountKey)).thenReturn(1L);
        when(valueOperations.increment(ipLimitKey)).thenReturn(1L);
        when(valueOperations.increment(emailIpLimitKey)).thenReturn(1L);

        assertFalse(emailService.checkVerificationCode(email, "000000", ip, VerifyCodeType.LOGIN));
        verify(redisTemplate).expire(errorCountKey, 5, TimeUnit.MINUTES);
        verify(redisTemplate).expire(ipLimitKey, 10, TimeUnit.MINUTES);
        verify(redisTemplate).expire(emailIpLimitKey, 10, TimeUnit.MINUTES);
    }

    @Test
    public void checkVerificationCodeShouldRejectWhenFailureCountReachedLimit() {
        String email = "example@qq.com";
        String ip = "127.0.0.1";
        String errorCountKey = RedisKey.verificationErrorCount(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);
        String emailIpLimitKey = RedisKey.verificationEmailIpRateLimit(VerifyCodeType.LOGIN.name(), email, ip);

        when(valueOperations.get(errorCountKey)).thenReturn("5");
        when(valueOperations.get(ipLimitKey)).thenReturn(null);
        when(valueOperations.get(emailIpLimitKey)).thenReturn(null);

        assertFalse(emailService.checkVerificationCode(email, "123456", ip, VerifyCodeType.LOGIN));
    }

    @Test
    public void sendVerificationCodeShouldRecordEmailIpRateLimitWhenAllowed() {
        String email = "example@qq.com";
        String ip = "127.0.0.1";
        String emailLimitKey = RedisKey.verificationLimitCode(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);
        String emailIpLimitKey = RedisKey.verificationEmailIpRateLimit(VerifyCodeType.LOGIN.name(), email, ip);

        when(valueOperations.get(emailLimitKey)).thenReturn(null);
        when(valueOperations.get(ipLimitKey)).thenReturn(null);
        when(valueOperations.get(emailIpLimitKey)).thenReturn(null);
        when(valueOperations.increment(ipLimitKey)).thenReturn(1L);
        when(valueOperations.increment(emailIpLimitKey)).thenReturn(1L);

        emailService.sendVerificationCode(email, ip, VerifyCodeType.LOGIN, true);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_ROUTING_KEY),
                isA(Object.class)
        );
        verify(valueOperations).set(emailLimitKey, "1", 60, TimeUnit.SECONDS);
        verify(redisTemplate).expire(ipLimitKey, 10, TimeUnit.MINUTES);
        verify(redisTemplate).expire(emailIpLimitKey, 10, TimeUnit.MINUTES);
    }

    @Test
    public void sendVerificationCodeShouldNotSendMailWhenUserDoesNotExist() {
        String email = "not-found@qq.com";
        String ip = "127.0.0.1";
        String emailLimitKey = RedisKey.verificationLimitCode(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);
        String emailIpLimitKey = RedisKey.verificationEmailIpRateLimit(VerifyCodeType.LOGIN.name(), email, ip);

        when(valueOperations.get(emailLimitKey)).thenReturn(null);
        when(valueOperations.get(ipLimitKey)).thenReturn(null);
        when(valueOperations.get(emailIpLimitKey)).thenReturn(null);
        when(valueOperations.increment(ipLimitKey)).thenReturn(1L);
        when(valueOperations.increment(emailIpLimitKey)).thenReturn(1L);

        assertNull(emailService.sendVerificationCode(email, ip, VerifyCodeType.LOGIN, false));
        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class),
                any(String.class),
                isA(Object.class)
        );
        verify(valueOperations).set(emailLimitKey, "1", 60, TimeUnit.SECONDS);
    }

    @Test
    public void sendVerificationCodeShouldRejectWhenEmailIpLimitReached() {
        String email = "example@qq.com";
        String ip = "127.0.0.1";
        String emailLimitKey = RedisKey.verificationLimitCode(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);
        String emailIpLimitKey = RedisKey.verificationEmailIpRateLimit(VerifyCodeType.LOGIN.name(), email, ip);

        when(valueOperations.get(emailLimitKey)).thenReturn(null);
        when(valueOperations.get(ipLimitKey)).thenReturn(null);
        when(valueOperations.get(emailIpLimitKey)).thenReturn("5");

        Executable action = () -> emailService.sendVerificationCode(email, ip, VerifyCodeType.LOGIN, true);

        assertThrows(RuntimeException.class, action);
        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class),
                any(String.class),
                isA(Object.class)
        );
    }

    @Test
    public void sendVerificationCodeShouldRejectWhenIpLimitReached() {
        String email = "example@qq.com";
        String ip = "127.0.0.1";
        String emailLimitKey = RedisKey.verificationLimitCode(VerifyCodeType.LOGIN.name(), email);
        String ipLimitKey = RedisKey.verificationIpRateLimit(VerifyCodeType.LOGIN.name(), ip);

        when(valueOperations.get(emailLimitKey)).thenReturn(null);
        when(valueOperations.get(ipLimitKey)).thenReturn("20");

        Executable action = () -> emailService.sendVerificationCode(email, ip, VerifyCodeType.LOGIN, true);

        assertThrows(RuntimeException.class, action);
        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class),
                any(String.class),
                isA(Object.class)
        );
    }
}
