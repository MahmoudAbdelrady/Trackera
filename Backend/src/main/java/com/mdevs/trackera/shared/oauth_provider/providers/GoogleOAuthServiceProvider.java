package com.mdevs.trackera.shared.oauth_provider.providers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthServiceProvider;
import com.mdevs.trackera.shared.utils.OAuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class GoogleOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.google.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.google.client-secret}")
    private String CLIENT_SECRET;

    private final static String REDIRECT_URI = AppConfig.getFrontendUrl() + "/oauth/google/callback";

    private final OAuthUtil oAuthUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    private final static Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthServiceProvider.class);

    @Autowired
    public GoogleOAuthServiceProvider(OAuthUtil oAuthUtil, RedisTemplate<String, Object> redisTemplate) {
        this.oAuthUtil = oAuthUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String getAuthFlowUrl(User user) {
        Map<String, String> securityParams = oAuthUtil.generateSecurityParams(user != null ? user.getId() : null);
        try {
            redisTemplate.opsForValue().set(securityParams.get("state"), securityParams, 10, TimeUnit.MINUTES);
            return UriComponentsBuilder
                    .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("client_id", CLIENT_ID)
                    .queryParam("redirect_uri", REDIRECT_URI)
                    .queryParam("response_type", "code")
                    .queryParam("scope", String.join(" ", "openid", "email", "profile"))
                    .queryParam("state", securityParams.get("state"))
                    .queryParam("code_challenge", securityParams.get("codeChallenge"))
                    .queryParam("code_challenge_method", "S256")
                    .queryParam("access_type", "offline")
                    .queryParam("include_granted_scopes", "true")
                    .queryParam("prompt", "consent")
                    .build().toString();
        } catch (Exception e) {
            LOGGER.error("Error while generating Google Auth URL", e);
            throw new RuntimeException("Something went wrong while linking Google account");
        }
    }

    @Override
    public OAuthUserInfoDTO authenticate(OAuthRequestDTO authRequest) {
        try {
            Map<String, String> securityParams = (Map<String, String>) redisTemplate.opsForValue().getAndDelete(authRequest.getState());
            if (securityParams == null || securityParams.isEmpty()) {
                throw new SecurityException("Invalid state parameter");
            }

            GoogleTokenResponse tokenResponse = getGoogleTokenResponse(authRequest.getAuthCode(), securityParams.get("codeVerifier"));
            GoogleIdTokenVerifier tokenVerifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()).setAudience(List.of(CLIENT_ID)).build();
            GoogleIdToken.Payload payload = tokenVerifier.verify(tokenResponse.getIdToken()).getPayload();
            Long userId = securityParams.containsKey("userId") ? Long.parseLong(securityParams.get("userId")) : null;
            OAuthAccessCredentialsDTO accessCredentialsDTO = new OAuthAccessCredentialsDTO(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getScope(), tokenResponse.getTokenType(), tokenResponse.getExpiresInSeconds());
            return new OAuthUserInfoDTO(userId, payload.getEmail(), (String) payload.get("given_name"), (String) payload.get("family_name"), (String) payload.get("picture"), OAuthProvider.GOOGLE, accessCredentialsDTO);
        } catch (Exception e) {
            LOGGER.error("Error while authenticating with Google: {}", e.getMessage(), e);
            throw new SecurityException("Error while authenticating with Google");
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        return "";
    }

    private GoogleTokenResponse getGoogleTokenResponse(String code, String codeVerifier) {
        try {
            return new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), CLIENT_ID, CLIENT_SECRET, code, REDIRECT_URI)
                    .set("code_verifier", codeVerifier).execute();
        } catch (Exception e) {
            LOGGER.error("Error while getting Google token response: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
