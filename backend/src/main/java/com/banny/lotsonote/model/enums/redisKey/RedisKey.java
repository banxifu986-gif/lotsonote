package com.banny.lotsonote.model.enums.redisKey;

import org.springframework.util.DigestUtils;

/**
 * Redis 键名管理类
 * 用于统一管理和生成 Redis 中使用的各种键名
 * 遵循 Redis 键名命名规范 : 使用冒号分隔的层级结构
 */
public class RedisKey {
    public static String verificationCode(String type, String email) {
        return "email:" + type.toLowerCase() + ":verification_code:" + email;
    }

    public static String verificationLimitCode(String type, String email) {
        return "email:" + type.toLowerCase() + ":verification_code:limit:" + email;
    }

    public static String verificationErrorCount(String type, String email) {
        return "email:" + type.toLowerCase() + ":verify_code:error_count:" + email;
    }

    public static String verificationIpRateLimit(String type, String ip) {
        return "email:" + type.toLowerCase() + ":verification_code:limit:ip:" + DigestUtils.md5DigestAsHex(ip.getBytes());
    }

    public static String verificationEmailIpRateLimit(String type, String email, String ip) {
        String rawKey = type + ":" + email + ":" + ip;
        return "email:" + type.toLowerCase() + ":verification_code:rate_limit:" + DigestUtils.md5DigestAsHex(rawKey.getBytes());
    }

    /**
     * 生成邮件任务队列的 Redis 键名
     * 
     * @return 格式为 "queue:email:task" 的 Redis 键名
     */
    public static String emailTaskQueue() {
        return "queue:email:task";
    }
}
