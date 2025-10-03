package com.mdevs.trackera.shared.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class OAuthUtil {
    private final TrackeraHasher trackeraHasher;

    @Autowired
    public OAuthUtil(TrackeraHasher trackeraHasher) {
        this.trackeraHasher = trackeraHasher;
    }

    public Map<String, String> generateSecurityParams(Long userId) {
        String state = generateState();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        Map<String, String> securityParams = new HashMap<>();
        securityParams.put("state", state);
        securityParams.put("codeVerifier", codeVerifier);
        securityParams.put("codeChallenge", codeChallenge);
        if (userId != null) {
            securityParams.put("userId", String.valueOf(userId));
        }
        return securityParams;
    }

    private String generateState() {
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(trackeraHasher.generateRandomString(24));
        joiner.add(String.valueOf(System.currentTimeMillis()));
        return trackeraHasher.encryptToBase64(joiner.toString(), true);
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
