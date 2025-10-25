package com.mdevs.trackera.shared.oauth_provider.providers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.UserOAuthProviderService;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthServiceProvider;
import com.mdevs.trackera.utils.OAuthUtil;
import com.mdevs.trackera.utils.TrackeraHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.google.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.google.client-secret}")
    private String CLIENT_SECRET;

    private final static Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthServiceProvider.class);

    public GoogleOAuthServiceProvider(UserOAuthProviderService userOAuthProviderService, RedisTemplate<String, Object> redisTemplate, TrackeraHasher trackeraHasher, OAuthUtil oAuthUtil) {
        super(userOAuthProviderService, redisTemplate, trackeraHasher, oAuthUtil);
    }

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String generateAuthFlowUrl(User user) {
        try {
            return getBaseOAuthBuilder("https://accounts.google.com/o/oauth2/v2/auth", CLIENT_ID, user, "openid", "email", "profile")
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
            Map<String, String> securityParams = validateAndGetSecurityParams(authRequest.getState());
            GoogleTokenResponse tokenResponse = getGoogleTokenResponse(authRequest.getAuthCode(), securityParams.get("codeVerifier"));
            GoogleIdTokenVerifier tokenVerifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()).setAudience(List.of(CLIENT_ID)).build();
            GoogleIdToken.Payload payload = tokenVerifier.verify(tokenResponse.getIdToken()).getPayload();
            Long userId = securityParams.containsKey("userId") ? Long.parseLong(securityParams.get("userId")) : null;
            OAuthAccessCredentialsDTO accessCredentialsDTO = new OAuthAccessCredentialsDTO(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getScope(), tokenResponse.getTokenType(), tokenResponse.getExpiresInSeconds());
            return new OAuthUserInfoDTO(userId, payload.getEmail(), (String) payload.get("given_name"), (String) payload.get("family_name"), (String) payload.get("picture"), accessCredentialsDTO, null);
        } catch (Exception e) {
            LOGGER.error("Error while authenticating with Google: {}", e.getMessage(), e);
            throw new SecurityException("Error while authenticating with Google");
        }
    }

    @Override
    public OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken) {
        return null;
    }

    private GoogleTokenResponse getGoogleTokenResponse(String code, String codeVerifier) {
        try {
            return new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), CLIENT_ID, CLIENT_SECRET, code, getRedirectUri())
                    .set("code_verifier", codeVerifier).execute();
        } catch (Exception e) {
            LOGGER.error("Error while getting Google token response: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
