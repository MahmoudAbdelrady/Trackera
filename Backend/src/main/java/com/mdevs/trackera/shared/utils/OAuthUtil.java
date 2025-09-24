package com.mdevs.trackera.shared.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class OAuthUtil {
    private final TrackeraHasher trackeraHasher;

    @Autowired
    public OAuthUtil(TrackeraHasher trackeraHasher) {
        this.trackeraHasher = trackeraHasher;
    }

    public Map<String, String> generateSecurityParams(String userEmail) {
        String state = generateState(userEmail);
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        return Map.of(
                "state", state,
                "codeVerifier", codeVerifier,
                "codeChallenge", codeChallenge
        );
    }

    private String generateState(String userEmail) {
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(trackeraHasher.generateRandomString(24));
        if (!StringUtils.isEmpty(userEmail)) {
            joiner.add(userEmail);
        }
        joiner.add(String.valueOf(System.currentTimeMillis()));
        return trackeraHasher.encryptToBase64(joiner.toString(), true);
    }

    public Map<String,String> parseOAuthState(String state) {
        String decryptedPayload = trackeraHasher.decryptFromBase64(state, true);
        String[] parts = decryptedPayload.split(":");
        return Collections.singletonMap("userEmail", parts.length > 2 ? parts[1] : null);
    }

    private String generateCodeVerifier() {
        // RFC 7636 requires 43–128 characters and 32 bytes will give ~43 chars Base64URL string
        return trackeraHasher.generateRandomString(32);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return trackeraHasher.encodeToBase64(hash, true);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
