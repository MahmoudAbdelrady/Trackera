package com.mdevs.trackera.shared.oauth_provider.providers;

import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.dto.jira.AccessibleResourceDTO;
import com.mdevs.trackera.service.JiraService;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthServiceProvider;
import com.mdevs.trackera.shared.utils.AppUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class JiraOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.jira.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.jira.client-secret}")
    private String CLIENT_SECRET;

    private final static String JIRA_AUTH_BASE_URL = "https://auth.atlassian.com";

    private final static Logger LOGGER = LoggerFactory.getLogger(JiraOAuthServiceProvider.class);

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.JIRA;
    }

    @Override
    public String generateAuthFlowUrl(HttpServletRequest httpRequest) {
        try {
            return getBaseOAuthBuilder(JIRA_AUTH_BASE_URL + "/authorize", CLIENT_ID, httpRequest, "read:jira-work", "read:jira-user", "write:jira-work", "offline_access")
                    .queryParam("audience", "api.atlassian.com")
                    .build().toString();
        } catch (Exception e) {
            LOGGER.error("Error while generating Jira Auth URL", e);
            throw new RuntimeException("Something went wrong while linking Jira account");
        }
    }

    @Override
    public OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest) {
        try {
            Map<String, String> securityParams = validateAndGetSecurityParams(authRequest.getState());
            OAuthAccessCredentialsDTO tokenResponse = getJiraTokenResponse(authRequest.getAuthCode(), securityParams.get("codeVerifier"));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokenResponse.getAccessToken());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = AppUtils.getRestTemplate();

            AccessibleResourceDTO accessibleResourceDTO = List.of(restTemplate.exchange("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, entity, AccessibleResourceDTO[].class).getBody()).getFirst();
            Map<String, Object> userJiraInfo = restTemplate.exchange(JiraService.JIRA_API_BASE_URL.replace("{cloudId}", accessibleResourceDTO.getId()) + "/myself", HttpMethod.GET, entity, Map.class).getBody();
            if (userJiraInfo == null || userJiraInfo.isEmpty() || !userJiraInfo.containsKey("accountId")) {
                throw new SecurityException("Failed to fetch user info from Jira");
            }
            Long userId = securityParams.containsKey("userId") ? Long.parseLong(securityParams.get("userId")) : null;
            String[] names = userJiraInfo.get("displayName").toString().split(" ");
            Map<String, Object> additionalInfo = new HashMap<>(Map.of(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY, Map.of("id", accessibleResourceDTO.getId(), "url", accessibleResourceDTO.getUrl())));

            return new OAuthUserInfoDTO(userId, userJiraInfo.get("emailAddress").toString(), names[0], names.length > 1 ? String.join(" ", names) : null,
                    userJiraInfo.get("avatarUrls") != null ? ((Map<String, String>) userJiraInfo.get("avatarUrls")).get("48x48") : null, tokenResponse, additionalInfo);
        } catch (Exception e) {
            LOGGER.error("Error while authenticating Jira Token", e);
            throw new SecurityException("Failed to authenticate with Jira");
        }
    }

    @Override
    public OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type",  "refresh_token");
            requestBody.put("client_id", CLIENT_ID);
            requestBody.put("client_secret", CLIENT_SECRET);
            requestBody.put("refresh_token", refreshToken);

            return getCredentialsFromJira(requestBody);
        } catch (RestClientException e) {
            LOGGER.error("Error while refreshing Jira access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with jira");
        }
    }

    public OAuthAccessCredentialsDTO getJiraTokenResponse(String authCode, String codeVerifier) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type",  "authorization_code");
            requestBody.put("client_id", CLIENT_ID);
            requestBody.put("client_secret", CLIENT_SECRET);
            requestBody.put("code", authCode);
            requestBody.put("code_verifier", codeVerifier);
            requestBody.put("redirect_uri", getRedirectUri());

            return getCredentialsFromJira(requestBody);
        } catch (RestClientException e) {
            LOGGER.error("Error while getting Jira token response: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private OAuthAccessCredentialsDTO getCredentialsFromJira(Map<String, String> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<OAuthAccessCredentialsDTO> response = AppUtils.getRestTemplate().exchange(JIRA_AUTH_BASE_URL + "/oauth/token", HttpMethod.POST, entity, OAuthAccessCredentialsDTO.class);

        return response.getBody();
    }
}
