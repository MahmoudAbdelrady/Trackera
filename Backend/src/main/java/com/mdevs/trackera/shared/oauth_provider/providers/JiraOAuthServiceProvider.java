package com.mdevs.trackera.shared.oauth_provider.providers;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthServiceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
public class JiraOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.jira.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.jira.client-secret}")
    private String CLIENT_SECRET;

    private final static String REDIRECT_URI = AppConfig.getBackendUrl() + "/auth/oauth-v2/jira/callback";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.JIRA;
    }

    @Override
    public String getAuthFlowUrl(User user) {
        return UriComponentsBuilder
                .fromUriString("https://auth.atlassian.com/authorize")
                .queryParam("audience", "api.atlassian.com")
                .queryParam("client_id", CLIENT_ID)
                .queryParam("scope", "read:jira-work read:jira-user write:jira-work offline_access")
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("response_type", "code")
                .build().toString();
    }

    @Override
    public OAuthAccessCredentialsDTO getAccessCredentials(String code, boolean isRefresh) {
        String tokenUrl = "https://auth.atlassian.com/oauth/token";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type",  isRefresh ? "refresh_token" : "authorization_code");
        requestBody.put("client_id", CLIENT_ID);
        requestBody.put("client_secret", CLIENT_SECRET);
        requestBody.put("code", code);
        requestBody.put("redirect_uri", REDIRECT_URI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<OAuthAccessCredentialsDTO> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, OAuthAccessCredentialsDTO.class);

        return response.getBody();
    }

    @Override
    public OAuthUserInfoDTO authenticate(String code) {
        return null;
    }
}
