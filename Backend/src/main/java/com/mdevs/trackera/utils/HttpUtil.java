package com.mdevs.trackera.utils;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public final class HttpUtil {
    private static final RestTemplate restTemplate = new RestTemplate();

    private HttpUtil() {}

    // ===== Core Exchange Method =====

    public static <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
        return restTemplate.exchange(url, method, entity, responseType);
    }

    // ===== Convenience Methods =====

    public static <T> T get(String url, HttpEntity<?> entity, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType).getBody();
    }

    public static <T> T post(String url, HttpEntity<?> entity, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType).getBody();
    }

    // ===== Entity Builders =====

    public static <T> HttpEntity<T> createBearerAuthEntity(String accessToken) {
        return new HttpEntity<>(createBearerAuthHeaders(accessToken, false));
    }

    public static <T> HttpEntity<T> createBearerAuthEntity(String accessToken, T body) {
        return new HttpEntity<>(body, createBearerAuthHeaders(accessToken, true));
    }

    public static <T> HttpEntity<T> createJsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static HttpHeaders createBearerAuthHeaders(String accessToken, boolean hasBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (hasBody) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }
}
