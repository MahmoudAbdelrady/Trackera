package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.service.UserOAuthProviderService;
import com.mdevs.trackera.utils.OAuthUtil;
import com.mdevs.trackera.utils.TrackeraHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
public abstract class OAuthServiceProvider {
    private final RedisTemplate<String, Object> redisTemplate;

    private final TrackeraHasher trackeraHasher;

    private final OAuthUtil oAuthUtil;

    //<editor-fold> common methods
    protected String getRedirectUri() {
        return AppConfig.getFrontendUrl() + "/oauth/" + getOAuthProvider().getCode() + "/callback";
    }

    protected UriComponentsBuilder getBaseOAuthBuilder(String authBaseUrl, String clientId, User user, String... scopes) {
        Map<String, String> securityParams = oAuthUtil.generateSecurityParams(user != null ? user.getId() : null);
        redisTemplate.opsForValue().set(securityParams.get("state"), securityParams, Duration.ofMinutes(10));

        return UriComponentsBuilder.fromUriString(authBaseUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", getRedirectUri())
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("response_type", "code")
                .queryParam("state", securityParams.get("state"))
                .queryParam("code_challenge", securityParams.get("codeChallenge"))
                .queryParam("code_challenge_method", "S256");
    }

    protected Map<String, String> validateAndGetSecurityParams(String state) {
        Map<String, String> securityParams = (Map<String, String>) redisTemplate.opsForValue().getAndDelete(state);
        if (securityParams == null || securityParams.isEmpty()) {
            throw new SecurityException("Invalid state parameter");
        }
        return securityParams;
    }

    public OAuthAccessCredentialsDTO refreshOAuthProviderCredentials(UserOAuthProvider userOAuthProvider) {
        try {
            return refreshAccessToken(trackeraHasher.decryptFromBase64(userOAuthProvider.getRefreshToken(), false));
        } catch (Exception e) {
            throw new SecurityException(e.getMessage());
        }
    }
    //</editor-fold>

    //<editor-fold> methods to be implemented by subclasses
    protected abstract OAuthProvider getOAuthProvider();

    public abstract String generateAuthFlowUrl(User user);

    public abstract OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest);

    protected abstract OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken);
    //</editor-fold>
}
