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
}
