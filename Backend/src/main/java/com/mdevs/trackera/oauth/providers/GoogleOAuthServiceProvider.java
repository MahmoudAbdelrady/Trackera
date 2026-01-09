package com.mdevs.trackera.oauth.providers;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.enums.OAuthProvider;
import com.mdevs.trackera.oauth.OAuthServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.google.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.google.client-secret}")
    private String CLIENT_SECRET;

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String generateAuthFlowUrl(User user) {
        return getBaseOAuthBuilder("https://accounts.google.com/o/oauth2/v2/auth", CLIENT_ID, user, "openid", "email", "profile")
                .queryParam("access_type", "offline")
                .queryParam("include_granted_scopes", "true")
                .queryParam("prompt", "consent")
                .build().toString();
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
            log.error("Error while authenticating with Google: {}", e.getMessage(), e);
            throw new SecurityException("Error while authenticating with Google");
        }
    }

    @Override
    public OAuthAccessCredentialsDTO refreshAccessToken(String refreshToken) throws Exception {
        TokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                refreshToken,
                CLIENT_ID,
                CLIENT_SECRET
        ).execute();

        return new OAuthAccessCredentialsDTO(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(), // could be null
                tokenResponse.getScope(),
                tokenResponse.getTokenType(),
                tokenResponse.getExpiresInSeconds()
        );
    }

    @Override
    public void handlePostLinkingActions(User user, OAuthUserInfoDTO userInfo) {
    }

    @Override
    public void handlePostUnLinkingActions(User user) {
    }

    private GoogleTokenResponse getGoogleTokenResponse(String code, String codeVerifier) {
        try {
            return new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), CLIENT_ID, CLIENT_SECRET, code, getRedirectUri())
                    .set("code_verifier", codeVerifier).execute();
        } catch (Exception e) {
            log.error("Error while getting Google token response: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
