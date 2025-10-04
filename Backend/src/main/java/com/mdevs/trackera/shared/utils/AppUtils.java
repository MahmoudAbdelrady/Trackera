package com.mdevs.trackera.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.web.client.RestTemplate;

public class AppUtils {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private static final RestTemplate restTemplate = new RestTemplate();

}
