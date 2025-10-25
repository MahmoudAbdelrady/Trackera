package com.mdevs.trackera.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class AppUtils {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private static final RestTemplate restTemplate = new RestTemplate();

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
}
