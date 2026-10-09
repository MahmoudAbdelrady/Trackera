package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.jira.JiraProjectDTO;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.utils.HttpUtil;
import com.mdevs.trackera.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JiraApiClient {
    private final OAuthConnectionService oAuthConnectionService;

    public static final String JIRA_API_BASE_URL = "https://api.atlassian.com/ex/jira/{cloudId}/rest/api/3";

    public static final String JIRA_ACCESSIBLE_RESOURCES_URL = "https://api.atlassian.com/oauth/token/accessible-resources";

    public static String getApiUrl(JiraProjectDTO projectDTO) {
        return JIRA_API_BASE_URL.replace("{cloudId}", projectDTO.getId());
    }

    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class}, maxRetries = 2, delay = 1000, multiplier = 3)
    public <T> T callJiraApi(String url, HttpMethod method, HttpEntity<?> entity, OAuthConnection oAuthConnection, Class<T> responseType) {
        try {
            ResponseEntity<T> response = HttpUtil.exchange(url, method, entity, responseType);

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                oAuthConnection = oAuthConnectionService.getOrRefresh(oAuthConnection);

                HttpHeaders newHeaders = new HttpHeaders();
                newHeaders.putAll(entity.getHeaders());
                newHeaders.setBearerAuth(oAuthConnectionService.getAccessToken(oAuthConnection));
                entity = new HttpEntity<>(newHeaders);

                response = HttpUtil.exchange(url, method, entity, responseType);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Jira API request failed with status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (HttpClientErrorException exception) {
            log.error("Error while calling Jira API with url: {}", url, exception);
            extractErrorAndThrow(exception);
            return null; // Unreachable, but required for compilation
        }
    }

    private void extractErrorAndThrow(HttpClientErrorException e) {
        String responseBody = e.getResponseBodyAsString();

        JsonNode root;
        try {
            root = JsonUtil.convertJsonStringToTree(responseBody);
        } catch (Exception parseErr) {
            throw new JiraException("Jira API Error: " + responseBody, e.getStatusCode().value());
        }

        // 1. handle "errorMessages" list
        if (root.has("errorMessages") && root.get("errorMessages").isArray() && !root.get("errorMessages").isEmpty()) {
            List<String> errors = new ArrayList<>();
            root.get("errorMessages").forEach(msg -> errors.add(msg.asString()));

            String message = String.join(" | ", errors);
            throw new JiraException(message, e.getStatusCode().value());
        }

        // 3. fallback unknown Jira error
        throw new JiraException("Jira API Error: " + responseBody, e.getStatusCode().value());
    }
}
