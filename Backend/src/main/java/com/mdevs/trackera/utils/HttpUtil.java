package com.mdevs.trackera.utils;

import lombok.Getter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class HttpUtil {
    @Getter
    private static final RestTemplate restTemplate = new RestTemplate();

    public static <T> HttpEntity<T> createBearerAuthEntity(String accessToken) {
        return new HttpEntity<>(createBearerAuthHeaders(accessToken));
    }

    public static <T> HttpEntity<T> createBearerAuthEntity(String accessToken, T body) {
        return new HttpEntity<>(body, createBearerAuthHeaders(accessToken));
    }

    private static HttpHeaders createBearerAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
