package com.mdevs.trackera.shared.oauth_provider.providers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthServiceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class GoogleOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.google.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.google.client-secret}")
    private String CLIENT_SECRET;

    private final static Logger LOGGER = Logger.getLogger(GoogleOAuthServiceProvider.class.getName());

    @Override
    protected OAuthProvider getOAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String getAuthFlowUrl(User user) {
        return null;
    }

    @Override
    public OAuthAccessCredentialsDTO getAccessCredentials(String code, boolean isRefresh) {
        GoogleTokenResponse googleTokenResponse = getGoogleTokenResponse(code);
        return new OAuthAccessCredentialsDTO(googleTokenResponse.getAccessToken(), googleTokenResponse.getRefreshToken(), googleTokenResponse.getScope(), googleTokenResponse.getTokenType(), googleTokenResponse.getExpiresInSeconds());
    }

    @Override
    public OAuthUserInfoDTO authenticate(String code) {
        try {
            GoogleTokenResponse tokenResponse = getGoogleTokenResponse(code);
            GoogleIdTokenVerifier tokenVerifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()).setAudience(List.of(CLIENT_ID)).build();
            GoogleIdToken.Payload payload = tokenVerifier.verify(tokenResponse.getIdToken()).getPayload();
            return new OAuthUserInfoDTO(payload.getEmail(), (String) payload.get("given_name"), (String) payload.get("family_name"), (String) payload.get("picture"), OAuthProvider.GOOGLE);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while authenticating with Google: " + e.getMessage(), e);
            throw new SecurityException("Error while authenticating with Google");
        }
    }

    private GoogleTokenResponse getGoogleTokenResponse(String code) {
        try {
            return new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), CLIENT_ID, CLIENT_SECRET, code, "postmessage").execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting Google token response: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
