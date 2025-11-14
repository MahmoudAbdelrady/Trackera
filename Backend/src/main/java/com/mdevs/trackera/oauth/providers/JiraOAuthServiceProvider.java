package com.mdevs.trackera.oauth.providers;

import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.dto.jira.JiraProjectDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.JiraService;
import com.mdevs.trackera.oauth.OAuthProvider;
import com.mdevs.trackera.oauth.OAuthServiceProvider;
import com.mdevs.trackera.utils.AppUtils;
import com.mdevs.trackera.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class JiraOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.jira.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.jira.client-secret}")
    private String CLIENT_SECRET;

    private static final String JIRA_AUTH_BASE_URL = "https://auth.atlassian.com";

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.JIRA;
    }

    @Override
    public String generateAuthFlowUrl(User user) {
        return getBaseOAuthBuilder(JIRA_AUTH_BASE_URL + "/authorize", CLIENT_ID, user, "read:jira-work", "read:jira-user", "write:jira-work", "offline_access")
                .queryParam("audience", "api.atlassian.com")
                .build().toString();
    }

    @Override
    public OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest) {
        try {
            Map<String, String> securityParams = validateAndGetSecurityParams(authRequest.getState());
            OAuthAccessCredentialsDTO tokenResponse = getJiraTokenResponse(authRequest.getAuthCode(), securityParams.get("codeVerifier"));

            HttpEntity<Void> entity = HttpUtil.createBearerAuthEntity(tokenResponse.getAccessToken());
            RestTemplate restTemplate = HttpUtil.getRestTemplate();

            JiraProjectDTO jiraProjectDTO = fetchJiraPrimaryProject(restTemplate, entity);
            Map<String, Object> userJiraInfo = fetchJiraUserInfo(jiraProjectDTO, restTemplate, entity);

            return buildJiraOAuthUserInfo(securityParams, tokenResponse, jiraProjectDTO, userJiraInfo);
        } catch (Exception e) {
            log.error("Error while authenticating Jira Token", e);
            throw new SecurityException("Failed to authenticate with Jira");
        }
    }

    @Override
    public OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "refresh_token");
        requestBody.put("client_id", CLIENT_ID);
        requestBody.put("client_secret", CLIENT_SECRET);
        requestBody.put("refresh_token", refreshToken);

        return getCredentialsFromJira(requestBody);
    }

    @Override
    public void handlePostLinkingActions(User user, OAuthUserInfoDTO userInfo) {
        String primaryProjectSetting = AppUtils.convertObjectToJsonString(userInfo.getAdditionalInfo().get(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY));
        userPreferenceService.create(user, JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY, primaryProjectSetting);
    }

    @Override
    public void handlePostUnLinkingActions(User user) {
        userPreferenceService.deleteByUserAndKey(user, JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY);
    }

    private OAuthAccessCredentialsDTO getJiraTokenResponse(String authCode, String codeVerifier) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "authorization_code");
            requestBody.put("client_id", CLIENT_ID);
            requestBody.put("client_secret", CLIENT_SECRET);
            requestBody.put("code", authCode);
            requestBody.put("code_verifier", codeVerifier);
            requestBody.put("redirect_uri", getRedirectUri());

            return getCredentialsFromJira(requestBody);
        } catch (RestClientException e) {
            log.error("Error while getting Jira token response: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private JiraProjectDTO fetchJiraPrimaryProject(RestTemplate restTemplate, HttpEntity<Void> entity) {
        JiraProjectDTO[] projects = restTemplate.exchange("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, entity, JiraProjectDTO[].class).getBody();
        if (projects == null || projects.length == 0) {
            throw new SecurityException("Failed to fetch Jira primary project");
        }
        return projects[0];
    }

    private Map<String, Object> fetchJiraUserInfo(JiraProjectDTO jiraProjectDTO, RestTemplate restTemplate, HttpEntity<Void> entity) {
        String url = JiraService.getApiUrl(jiraProjectDTO) + "/myself";
        Map<String, Object> userJiraInfo = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class).getBody();
        if (userJiraInfo == null || userJiraInfo.isEmpty() || !userJiraInfo.containsKey("accountId")) {
            throw new SecurityException("Failed to fetch user info from Jira");
        }
        return userJiraInfo;
    }

    private OAuthUserInfoDTO buildJiraOAuthUserInfo(Map<String, String> securityParams, OAuthAccessCredentialsDTO tokenResponse, JiraProjectDTO primaryProject, Map<String, Object> userJiraInfo) {
        Long userId = securityParams.containsKey("userId") ? Long.parseLong(securityParams.get("userId")) : null;
        String[] names = parseDisplayName((String) userJiraInfo.get("displayName"));
        String avatarUrl = userJiraInfo.get("avatarUrls") != null && (userJiraInfo.get("avatarUrls") instanceof Map) ? ((Map<String, String>) userJiraInfo.get("avatarUrls")).get("48x48") : null;
        Map<String, Object> additionalInfo = new HashMap<>(Map.of(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY, primaryProject));

        return new OAuthUserInfoDTO(userId, userJiraInfo.get("emailAddress").toString(), names[0], names[1], avatarUrl, tokenResponse, additionalInfo);
    }

    private String[] parseDisplayName(String displayName) {
        if (StringUtils.isEmpty(displayName)) {
            return new String[]{"Unknown", null};
        }

        String[] parts = displayName.trim().split("\\s+", 2);
        return new String[]{
                parts[0],
                parts.length > 1 ? parts[1] : null
        };
    }

    private OAuthAccessCredentialsDTO getCredentialsFromJira(Map<String, String> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<OAuthAccessCredentialsDTO> response = HttpUtil.getRestTemplate().exchange(JIRA_AUTH_BASE_URL + "/oauth/token", HttpMethod.POST, entity, OAuthAccessCredentialsDTO.class);

        return response.getBody();
    }
}
