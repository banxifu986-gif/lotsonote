package com.banny.lotsonote.model.enums.redisKey;

import org.springframework.util.DigestUtils;

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

    public static String emailTaskDone(String taskId) {
        return "email:task:done:" + taskId;
    }
}
