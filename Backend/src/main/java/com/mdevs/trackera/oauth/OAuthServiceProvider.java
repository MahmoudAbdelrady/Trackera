package com.mdevs.trackera.oauth;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.service.UserPreferenceService;
import com.mdevs.trackera.shared.CacheService;
import com.mdevs.trackera.shared.enums.OAuthProvider;
import com.mdevs.trackera.utils.OAuthUtil;
import com.mdevs.trackera.utils.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@Slf4j
public abstract class OAuthServiceProvider {
    @Autowired
    protected CacheService cacheService;

    @Autowired
    protected UserPreferenceService userPreferenceService;

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private OAuthUtil oAuthUtil;

    // Public API
    public abstract String generateAuthFlowUrl(User user);

    public abstract OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest);

    public abstract void handlePostLinkingActions(User user, OAuthUserInfoDTO userInfo);

    public OAuthAccessCredentialsDTO refreshOAuthProviderCredentials(OAuthConnection OAuthConnection) {
        try {
            return refreshAccessToken(cryptoUtil.decryptFromBase64(OAuthConnection.getRefreshToken(), false));
        } catch (Exception e) {
            log.error("Error while refreshing {} access token: {}", getOAuthProvider().getDisplayName(), e.getMessage(), e);
            throw new SecurityException("Failed to authenticate with " + getOAuthProvider().getDisplayName());
        }
    }

    public abstract void handlePostUnLinkingActions(User user);

    // Protected API (for subclasses)
    protected abstract OAuthProvider getOAuthProvider();

    protected String getRedirectUri() {
        return AppConfig.getFrontendUrl() + "/oauth/" + getOAuthProvider().getCode() + "/callback";
    }

    protected UriComponentsBuilder getBaseOAuthBuilder(String authBaseUrl, String clientId, User user, String... scopes) {
        try {
            Map<String, String> securityParams = oAuthUtil.generateSecurityParams(user != null ? user.getId() : null);
            cacheService.set(securityParams.get("state"), securityParams, Duration.ofMinutes(10));

            return UriComponentsBuilder.fromUriString(authBaseUrl)
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", getRedirectUri())
                    .queryParam("scope", String.join(" ", scopes))
                    .queryParam("response_type", "code")
                    .queryParam("state", securityParams.get("state"))
                    .queryParam("code_challenge", securityParams.get("codeChallenge"))
                    .queryParam("code_challenge_method", "S256");
        } catch (Exception e) {
            log.error("Error while generating {} Auth URL", getOAuthProvider().getDisplayName(), e);
            throw new RuntimeException("Something went wrong while linking " + getOAuthProvider().getDisplayName() + " account");
        }
    }

    protected Map<String, String> validateAndGetSecurityParams(String state) {
        Map<String, String> securityParams = (Map<String, String>) cacheService.get(state, Map.class);
        if (securityParams == null || securityParams.isEmpty()) {
            throw new SecurityException("Invalid state parameter");
        }
        cacheService.evict(state);
        return securityParams;
    }

    protected abstract OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken) throws Exception;
}
