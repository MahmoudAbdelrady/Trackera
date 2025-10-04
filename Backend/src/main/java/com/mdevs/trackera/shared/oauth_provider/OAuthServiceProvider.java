package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.utils.JwtUtil;
import com.mdevs.trackera.shared.utils.OAuthUtil;
import com.mdevs.trackera.shared.utils.TrackeraHasher;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class OAuthServiceProvider {
    //<editor-fold> common methods
    private User validateAndGetAuthFlowUser(HttpServletRequest request) {
        String jwtTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isEmpty(jwtTokenHeader) || !jwtTokenHeader.startsWith("Bearer ")) {
            return null;
        }
        JwtUtil jwtUtil = AppConfig.getApplicationContext().getBean(JwtUtil.class);
        UserRepository userRepository = AppConfig.getApplicationContext().getBean(UserRepository.class);
        UserOAuthProviderRepository userOAuthProviderRepository = AppConfig.getApplicationContext().getBean(UserOAuthProviderRepository.class);

        Claims claims = jwtUtil.validateAndGetTokenPayload(jwtTokenHeader.substring(7), true);
        User user = userRepository.findByEmail(claims.get("email", String.class));
        if (userOAuthProviderRepository.existsByUserAndProvider(user, getOAuthProvider())) {
            throw new BusinessException("The current account is already linked with " + getOAuthProvider().getDisplayName());
        }
        return user;
    }

    protected String getRedirectUri() {
        return AppConfig.getFrontendUrl() + "/oauth/" + getOAuthProvider().getCode() + "/callback";
    }

    protected UriComponentsBuilder getBaseOAuthBuilder(String authBaseUrl, String clientId, HttpServletRequest httpRequest, String... scopes) {
        User user = validateAndGetAuthFlowUser(httpRequest);
        Map<String, String> securityParams = AppConfig.getApplicationContext().getBean(OAuthUtil.class).generateSecurityParams(user != null ? user.getId() : null);
        AppConfig.getApplicationContext().getBean(RedisTemplate.class).opsForValue().set(securityParams.get("state"), securityParams, 10, TimeUnit.MINUTES);

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
        Map<String, String> securityParams = (Map<String, String>) AppConfig.getApplicationContext().getBean(RedisTemplate.class).opsForValue().getAndDelete(state);
        if (securityParams == null || securityParams.isEmpty()) {
            throw new SecurityException("Invalid state parameter");
        }
        return securityParams;
    }

    public String refreshOAuthProviderCredentials(UserOAuthProvider userOAuthProvider) {
        try {
            TrackeraHasher trackeraHasher = AppConfig.getApplicationContext().getBean(TrackeraHasher.class);
            OAuthAccessCredentialsDTO newTokens = refreshAccessToken(trackeraHasher.decryptFromBase64(userOAuthProvider.getRefreshToken(), false));

            userOAuthProvider.setAccessToken(trackeraHasher.encryptToBase64(newTokens.getAccessToken(), false));
            userOAuthProvider.setRefreshToken(trackeraHasher.encryptToBase64(newTokens.getRefreshToken(), false));
            userOAuthProvider.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(newTokens.getExpiresIn()));
            AppConfig.getApplicationContext().getBean(UserOAuthProviderRepository.class).save(userOAuthProvider);

            return newTokens.getAccessToken();
        } catch (Exception e) {
            throw new SecurityException(e.getMessage());
        }
    }
    //</editor-fold>

    //<editor-fold> methods to be implemented by subclasses
    protected abstract OAuthProvider getOAuthProvider();

    public abstract String generateAuthFlowUrl(HttpServletRequest httpRequest);

    public abstract OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest);

    protected abstract OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken);
    //</editor-fold>
}
