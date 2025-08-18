package com.mdevs.trackera.shared.oauth_provider;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleOAuthServiceProvider extends OAuthServiceProvider {
    @Value("${trackera.oauth2.google.client-id}")
    private String CLIENT_ID;

    @Value("${trackera.oauth2.google.client-secret}")
    private String CLIENT_SECRET;

    @Override
    public OAuthUserInfoDTO authenticate(String tokenCode) {
        try {
            GoogleTokenResponse tokenResponse = getGoogleTokenResponse(tokenCode);
            GoogleIdTokenVerifier tokenVerifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()).setAudience(List.of(CLIENT_ID)).build();
            GoogleIdToken.Payload payload = tokenVerifier.verify(tokenResponse.getIdToken()).getPayload();
            return new OAuthUserInfoDTO(payload.getEmail(), (String) payload.get("given_name"), (String) payload.get("family_name"), (String) payload.get("picture"), OAuthProvider.GOOGLE);
        } catch (Exception e) {
            throw new SecurityException("Error while authenticating with Google");
        }
    }

    private GoogleTokenResponse getGoogleTokenResponse(String tokenCode) {
        try {
            return new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), CLIENT_ID, CLIENT_SECRET, tokenCode, "postmessage").execute();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
