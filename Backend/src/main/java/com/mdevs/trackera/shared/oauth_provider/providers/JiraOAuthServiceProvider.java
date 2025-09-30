package com.mdevs.trackera.shared.oauth_provider.providers;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.dto.jira.AccessibleResourceDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthServiceProvider;
import com.mdevs.trackera.shared.utils.OAuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class JiraOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.jira.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.jira.client-secret}")
    private String CLIENT_SECRET;

    private final static String REDIRECT_URI = AppConfig.getFrontendUrl() + "/oauth/jira/callback";

    private final static String JIRA_AUTH_BASE_URL = "https://auth.atlassian.com";

    private final static String JIRA_API_BASE_URL = "https://api.atlassian.com/ex/jira/{cloudId}/rest/api/3";

    private final RestTemplate restTemplate = new RestTemplate();

    private final RedisTemplate<String, Object> redisTemplate;

    private final OAuthUtil oAuthUtil;

    private final static Logger LOGGER = LoggerFactory.getLogger(JiraOAuthServiceProvider.class);

    @Autowired
    public JiraOAuthServiceProvider(RedisTemplate<String, Object> redisTemplate, OAuthUtil oAuthUtil) {
        this.redisTemplate = redisTemplate;
        this.oAuthUtil = oAuthUtil;
    }

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.JIRA;
    }

    @Override
    public String getAuthFlowUrl(User user) {
        Map<String, String> securityParams = oAuthUtil.generateSecurityParams(user != null ? user.getId() : null);
        try {
            redisTemplate.opsForValue().set(securityParams.get("state"), securityParams, 10, TimeUnit.MINUTES);
            return UriComponentsBuilder
                    .fromUriString(JIRA_AUTH_BASE_URL + "/authorize")
                    .queryParam("audience", "api.atlassian.com")
                    .queryParam("client_id", CLIENT_ID)
                    .queryParam("scope", "read:jira-work read:jira-user write:jira-work offline_access")
                    .queryParam("redirect_uri", REDIRECT_URI)
                    .queryParam("state", securityParams.get("state"))
                    .queryParam("response_type", "code")
                    .queryParam("code_challenge", securityParams.get("codeChallenge"))
                    .queryParam("code_challenge_method", "S256")
                    .build().toString();
        } catch (Exception e) {
            LOGGER.error("Error while generating Jira Auth URL", e);
            throw new RuntimeException("Something went wrong while linking Jira account");
        }
    }

    @Override
    public OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest) {
        try {
            Map<String, String> securityParams = (Map<String, String>) redisTemplate.opsForValue().getAndDelete(authRequest.getState());
            if (securityParams == null || securityParams.isEmpty()) {
                throw new SecurityException("Invalid state parameter");
            }

            OAuthAccessCredentialsDTO tokenResponse = getJiraTokenResponse(authRequest.getAuthCode(), securityParams.get("codeVerifier"));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokenResponse.getAccessToken());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            AccessibleResourceDTO accessibleResourceDTO = List.of(restTemplate.exchange("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, entity, AccessibleResourceDTO[].class).getBody()).getFirst();
            Map<String, Object> userJiraInfo = restTemplate.exchange(JIRA_API_BASE_URL.replace("{cloudId}", accessibleResourceDTO.getId()) + "/myself", HttpMethod.GET, entity, Map.class).getBody();
            if (userJiraInfo == null || userJiraInfo.isEmpty() || !userJiraInfo.containsKey("accountId")) {
                throw new SecurityException("Failed to fetch user info from Jira");
            }
            Long userId = securityParams.containsKey("userId") ? Long.parseLong(securityParams.get("userId")) : null;
            String[] names = userJiraInfo.get("displayName").toString().split(" ");

            return new OAuthUserInfoDTO(userId, userJiraInfo.get("emailAddress").toString(), names[0], names.length > 1 ? String.join(" ", names) : null, userJiraInfo.get("avatarUrls") != null ? ((Map<String, String>) userJiraInfo.get("avatarUrls")).get("48x48") : null, OAuthProvider.JIRA, tokenResponse);
        } catch (Exception e) {
            LOGGER.error("Error while authenticating Jira Token", e);
            throw new SecurityException("Failed to authenticate with Jira");
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        return "";
    }

    public OAuthAccessCredentialsDTO getJiraTokenResponse(String authCode, String codeVerifier) {
        String tokenUrl = JIRA_AUTH_BASE_URL + "/oauth/token";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type",  "authorization_code");
        requestBody.put("client_id", CLIENT_ID);
        requestBody.put("client_secret", CLIENT_SECRET);
        requestBody.put("code", authCode);
        requestBody.put("code_verifier", codeVerifier);
        requestBody.put("redirect_uri", REDIRECT_URI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<OAuthAccessCredentialsDTO> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, OAuthAccessCredentialsDTO.class);

        return response.getBody();
    }
}
