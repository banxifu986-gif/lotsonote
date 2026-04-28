package com.banny.lotsonote.utils;

import java.security.SecureRandom;

public class RandomCodeUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateNumberCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }
}
