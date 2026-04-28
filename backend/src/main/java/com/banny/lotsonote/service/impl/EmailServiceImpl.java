package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.model.enums.redisKey.RedisKey;
import com.banny.lotsonote.model.enums.user.VerifyCodeType;
import com.banny.lotsonote.service.EmailService;
import com.banny.lotsonote.task.email.EmailTask;
import com.banny.lotsonote.utils.RandomCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${mail.verify-code.limit-expire-seconds}")
    private int limitExpireSeconds;

    @Value("${mail.verify-code.max-error-count}")
    private int maxErrorCount;

    @Value("${mail.verify-code.error-expire-minutes}")
    private int errorExpireMinutes;

    @Value("${mail.verify-code.email-ip-limit-count}")
    private int emailIpLimitCount;

    @Value("${mail.verify-code.email-ip-limit-expire-minutes}")
    private int emailIpLimitExpireMinutes;

    @Value("${mail.verify-code.ip-limit-count}")
    private int ipLimitCount;

    @Value("${mail.verify-code.ip-limit-expire-minutes}")
    private int ipLimitExpireMinutes;

    @Override
    public String sendVerificationCode(String email, String ip, VerifyCodeType type, boolean shouldSend) {
        if (isVerificationCodeRateLimited(email, type)) {
            throw new BusinessException("验证码发送太频繁，请 60 秒后重试");
        }

        String ipRateLimitKey = RedisKey.verificationIpRateLimit(type.name(), ip);
        String ipRateLimitCount = redisTemplate.opsForValue().get(ipRateLimitKey);
        if (ipRateLimitCount != null && Integer.parseInt(ipRateLimitCount) >= ipLimitCount) {
            throw new BusinessException("当前 IP 请求过于频繁，请稍后重试");
        }

        String emailIpRateLimitKey = RedisKey.verificationEmailIpRateLimit(type.name(), email, ip);
        String emailIpRateLimitCount = redisTemplate.opsForValue().get(emailIpRateLimitKey);
        if (emailIpRateLimitCount != null && Integer.parseInt(emailIpRateLimitCount) >= emailIpLimitCount) {
            throw new BusinessException("验证码发送过于频繁，请稍后重试");
        }

        String verificationCode = shouldSend ? RandomCodeUtil.generateNumberCode(6) : null;

        try {
            if (shouldSend) {
                EmailTask emailTask = new EmailTask();
                emailTask.setEmail(email);
                emailTask.setCode(verificationCode);
                emailTask.setType(type.name());
                emailTask.setTimestamp(System.currentTimeMillis());
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EMAIL_EXCHANGE,
                        RabbitMQConfig.EMAIL_ROUTING_KEY,
                        emailTask
                );
                log.info("邮件任务已发送到RabbitMQ队列: {}", email);
            }

            String emailLimitKey = RedisKey.verificationLimitCode(type.name(), email);
            redisTemplate.opsForValue().set(emailLimitKey, "1", limitExpireSeconds, TimeUnit.SECONDS);
            Long ipCount = redisTemplate.opsForValue().increment(ipRateLimitKey);
            if (ipCount != null && ipCount == 1) {
                redisTemplate.expire(ipRateLimitKey, ipLimitExpireMinutes, TimeUnit.MINUTES);
            }
            Long count = redisTemplate.opsForValue().increment(emailIpRateLimitKey);
            if (count != null && count == 1) {
                redisTemplate.expire(emailIpRateLimitKey, emailIpLimitExpireMinutes, TimeUnit.MINUTES);
            }

            return verificationCode;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送验证码邮件失败", e);
            throw new BusinessException("发送验证码失败，请稍后重试");
        }
    }

    @Override
    public boolean checkVerificationCode(String email, String code, String ip, VerifyCodeType type) {
        String redisKey = RedisKey.verificationCode(type.name(), email);
        String errorCountKey = RedisKey.verificationErrorCount(type.name(), email);
        String ipRateLimitKey = RedisKey.verificationIpRateLimit(type.name(), ip);
        String emailIpRateLimitKey = RedisKey.verificationEmailIpRateLimit(type.name(), email, ip);
        String errorCount = redisTemplate.opsForValue().get(errorCountKey);
        String ipRateLimitCount = redisTemplate.opsForValue().get(ipRateLimitKey);
        String emailIpRateLimitCount = redisTemplate.opsForValue().get(emailIpRateLimitKey);

        if (errorCount != null && Integer.parseInt(errorCount) >= maxErrorCount) {
            return false;
        }
        if (ipRateLimitCount != null && Integer.parseInt(ipRateLimitCount) >= ipLimitCount) {
            return false;
        }
        if (emailIpRateLimitCount != null && Integer.parseInt(emailIpRateLimitCount) >= emailIpLimitCount) {
            return false;
        }

        String verificationCode = redisTemplate.opsForValue().get(redisKey);

        if (verificationCode != null && verificationCode.equals(code)) {
            redisTemplate.delete(redisKey);
            redisTemplate.delete(errorCountKey);
            return true;
        }

        Long count = redisTemplate.opsForValue().increment(errorCountKey);
        if (count != null && count == 1) {
            redisTemplate.expire(errorCountKey, errorExpireMinutes, TimeUnit.MINUTES);
        }
        Long ipCount = redisTemplate.opsForValue().increment(ipRateLimitKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(ipRateLimitKey, ipLimitExpireMinutes, TimeUnit.MINUTES);
        }
        Long emailIpCount = redisTemplate.opsForValue().increment(emailIpRateLimitKey);
        if (emailIpCount != null && emailIpCount == 1) {
            redisTemplate.expire(emailIpRateLimitKey, emailIpLimitExpireMinutes, TimeUnit.MINUTES);
        }
        return false;
    }

    @Override
    public boolean isVerificationCodeRateLimited(String email, VerifyCodeType type) {
        String redisKey = RedisKey.verificationLimitCode(type.name(), email);
        return redisTemplate.opsForValue().get(redisKey) != null;
    }
}
