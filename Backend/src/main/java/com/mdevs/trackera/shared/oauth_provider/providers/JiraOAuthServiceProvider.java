package com.mdevs.trackera.shared.oauth_provider.providers;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthV2RequestDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final static Logger logger = LoggerFactory.getLogger(JiraOAuthServiceProvider.class);

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
        // @TODO --> need to handle concurrent logins with same user/no user
        redisTemplate.opsForValue().set(securityParams.get("state"), securityParams);
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
    }

    @Override
    public OAuthUserInfoDTO authenticate(String code) {
        return null;
    }

    @Override
    public OAuthUserInfoDTO authenticateV2(OAuthV2RequestDTO authRequest) {
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

            ResponseEntity<AccessibleResourceDTO[]> response = restTemplate.exchange("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, entity, AccessibleResourceDTO[].class);
            AccessibleResourceDTO accessibleResourceDTO = List.of(response.getBody()).getFirst();

            ResponseEntity<Map> userJiraInfo = restTemplate.exchange(JIRA_API_BASE_URL.replace("{cloudId}", accessibleResourceDTO.getId()) + "/myself", HttpMethod.GET, entity, Map.class);
            Map<String, Object> userInfo = userJiraInfo.getBody();
            Long userId = securityParams.containsKey("userId") ? Long.parseLong(securityParams.get("userId")) : null;

            return new OAuthUserInfoDTO(userId, userInfo.get("emailAddress").toString(), userInfo.get("displayName").toString(), null, userInfo.get("avatarUrls") != null ? ((Map<String, String>) userInfo.get("avatarUrls")).get("48x48") : "", OAuthProvider.JIRA, tokenResponse);
        } catch (Exception e) {
            logger.error("Error while authenticating Jira Token", e);
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
