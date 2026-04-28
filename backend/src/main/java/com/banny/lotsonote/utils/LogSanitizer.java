package com.banny.lotsonote.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LogSanitizer {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
    private static final Set<String> OMITTED_FIELDS = Set.of("password", "token", "verifycode");
    private static final Set<String> EMAIL_FIELDS = Set.of("email");
    private static final Set<String> PHONE_FIELDS = Set.of("phone", "mobile", "phonenumber", "telephone");

    private LogSanitizer() {
    }

    public static String sanitize(Object value) {
        Object sanitizedValue = sanitizeValue(value, null, new IdentityHashMap<>());
        try {
            return OBJECT_MAPPER.writeValueAsString(sanitizedValue);
        } catch (JsonProcessingException e) {
            return String.valueOf(sanitizedValue);
        }
    }

    public static String sanitizeArguments(String[] parameterNames, Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        if (parameterNames == null || parameterNames.length != args.length) {
            return sanitize(args);
        }

        Map<String, Object> sanitizedArgs = new LinkedHashMap<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String parameterName = parameterNames[i];
            Object sanitizedArg = sanitizeValue(args[i], parameterName, visited);
            if (sanitizedArg != null) {
                sanitizedArgs.put(parameterName, sanitizedArg);
            }
        }
        return sanitize(sanitizedArgs);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        int separatorIndex = email.indexOf('@');
        if (separatorIndex <= 0 || separatorIndex == email.length() - 1) {
            return maskMiddle(email);
        }

        String localPart = email.substring(0, separatorIndex);
        String domainPart = email.substring(separatorIndex);
        return maskLocalPart(localPart) + domainPart;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        String trimmedPhone = phone.trim();
        if (trimmedPhone.length() >= 11) {
            return trimmedPhone.substring(0, 3) + "****" + trimmedPhone.substring(trimmedPhone.length() - 4);
        }
        if (trimmedPhone.length() <= 2) {
            return "*".repeat(trimmedPhone.length());
        }
        if (trimmedPhone.length() <= 6) {
            return trimmedPhone.charAt(0) + "***" + trimmedPhone.charAt(trimmedPhone.length() - 1);
        }
        return trimmedPhone.substring(0, 2) + "***" + trimmedPhone.substring(trimmedPhone.length() - 2);
    }

    private static Object sanitizeValue(Object value, String fieldName, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            return null;
        }
        if (shouldOmit(fieldName)) {
            return null;
        }
        if (value instanceof CharSequence sequence) {
            return maskString(fieldName, sequence.toString());
        }
        if (isSimpleValue(value)) {
            return value;
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(item -> sanitizeValue(item, fieldName, visited)).orElse(null);
        }
        if (value instanceof MultipartFile file) {
            Map<String, Object> fileInfo = new LinkedHashMap<>();
            fileInfo.put("name", file.getName());
            fileInfo.put("originalFilename", file.getOriginalFilename());
            fileInfo.put("size", file.getSize());
            return fileInfo;
        }
        if (value instanceof ServletRequest) {
            return "[ServletRequest]";
        }
        if (value instanceof ServletResponse) {
            return "[ServletResponse]";
        }
        if (value instanceof BindingResult) {
            return "[BindingResult]";
        }
        if (visited.containsKey(value)) {
            return value.getClass().getSimpleName();
        }

        visited.put(value, Boolean.TRUE);
        try {
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                List<Object> sanitizedList = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    Object item = sanitizeValue(Array.get(value, i), null, visited);
                    if (item != null) {
                        sanitizedList.add(item);
                    }
                }
                return sanitizedList;
            }

            if (value instanceof Collection<?> collection) {
                List<Object> sanitizedList = new ArrayList<>(collection.size());
                for (Object item : collection) {
                    Object sanitizedItem = sanitizeValue(item, null, visited);
                    if (sanitizedItem != null) {
                        sanitizedList.add(sanitizedItem);
                    }
                }
                return sanitizedList;
            }

            if (value instanceof Map<?, ?> map) {
                Map<String, Object> sanitizedMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    Object sanitizedEntryValue = sanitizeValue(entry.getValue(), key, visited);
                    if (sanitizedEntryValue != null) {
                        sanitizedMap.put(key, sanitizedEntryValue);
                    }
                }
                return sanitizedMap;
            }

            return sanitizeBean(value, visited);
        } finally {
            visited.remove(value);
        }
    }

    private static Map<String, Object> sanitizeBean(Object bean, IdentityHashMap<Object, Boolean> visited) {
        Map<String, Object> sanitizedBean = new LinkedHashMap<>();
        for (PropertyDescriptor descriptor : getPropertyDescriptors(bean.getClass())) {
            Method readMethod = descriptor.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            try {
                Object propertyValue = readMethod.invoke(bean);
                Object sanitizedPropertyValue = sanitizeValue(propertyValue, descriptor.getName(), visited);
                if (sanitizedPropertyValue != null) {
                    sanitizedBean.put(descriptor.getName(), sanitizedPropertyValue);
                }
            } catch (Exception ignored) {
            }
        }
        return sanitizedBean;
    }

    private static PropertyDescriptor[] getPropertyDescriptors(Class<?> type) {
        try {
            return Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            return new PropertyDescriptor[0];
        }
    }

    private static boolean shouldOmit(String fieldName) {
        String normalizedFieldName = normalize(fieldName);
        if (normalizedFieldName.isEmpty()) {
            return false;
        }
        for (String omittedField : OMITTED_FIELDS) {
            if (normalizedFieldName.contains(omittedField)) {
                return true;
            }
        }
        return false;
    }

    private static String maskString(String fieldName, String value) {
        String normalizedFieldName = normalize(fieldName);
        if (EMAIL_FIELDS.contains(normalizedFieldName)) {
            return maskEmail(value);
        }
        if (PHONE_FIELDS.contains(normalizedFieldName)) {
            return maskPhone(value);
        }
        return value;
    }

    private static boolean isSimpleValue(Object value) {
        return value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof TemporalAccessor
                || value instanceof Date
                || value instanceof Class<?>;
    }

    private static String normalize(String fieldName) {
        if (fieldName == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(fieldName.length());
        for (char currentChar : fieldName.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(currentChar)) {
                builder.append(currentChar);
            }
        }
        return builder.toString();
    }

    private static String maskLocalPart(String localPart) {
        if (localPart.length() <= 1) {
            return "*";
        }
        if (localPart.length() == 2) {
            return localPart.charAt(0) + "*";
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
    }

    private static String maskMiddle(String value) {
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }
}
