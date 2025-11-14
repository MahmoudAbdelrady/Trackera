package com.mdevs.trackera.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class AppUtils {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String convertObjectToJsonString(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T convertJsonStringToObject(String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // If already the correct type, just cast
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }

        // Convert from String if needed
        if (value instanceof String str) {
            String s = str.trim();
            if (targetType == Integer.class) {
                return targetType.cast(Integer.valueOf(s));
            } else if (targetType == Long.class) {
                return targetType.cast(Long.valueOf(s));
            } else if (targetType == Double.class) {
                return targetType.cast(Double.valueOf(s));
            } else if (targetType == Boolean.class) {
                return targetType.cast(Boolean.valueOf(s));
            } else if (targetType == String.class) {
                return targetType.cast(s);
            } else {
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
            }
        }

        // Convert numbers to other numeric types
        if (value instanceof Number number) {
            if (targetType == Integer.class) return targetType.cast(number.intValue());
            if (targetType == Long.class) return targetType.cast(number.longValue());
            if (targetType == Double.class) return targetType.cast(number.doubleValue());
        }

        // Cannot convert
        throw new ClassCastException("Cannot convert value of type " + value.getClass() + " to " + targetType);
    }
}
