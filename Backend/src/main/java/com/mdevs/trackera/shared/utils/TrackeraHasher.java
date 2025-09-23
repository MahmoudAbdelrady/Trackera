package com.mdevs.trackera.shared.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Component
public class TrackeraHasher {

    @Value("${trackera.hasher_secret_key}")
    private String hasherSecretKey;

    @Value("${trackera.hasher.encryption_key}")
    private String encryptionSecretKey;

    private final static String ENCRYPTION_ALGORITHM = "AES";

    private final static SecureRandom secureRandom = new SecureRandom();

    private SecretKey getAesKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(encryptionSecretKey), "AES");
    }

    public String hash(String text, boolean isUrl) {
        try {
            byte[] encryptedData = encrypt(text);
            Base64.Encoder encoder = isUrl ? Base64.getUrlEncoder() : Base64.getEncoder();
            return encoder.withoutPadding().encodeToString(hmacHashByteData(encryptedData));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMatch(String text, String hash, boolean isUrl) {
        try {
            byte[] encryptedData = encrypt(text);
            byte[] expectedHash = isUrl ? Base64.getUrlDecoder().decode(hash) : Base64.getDecoder().decode(hash);
            byte[] actualHash = hmacHashByteData(encryptedData);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String hashForSecurityToken(Long secTokenId) {
        try {
            byte[] encryptedId = encrypt(secTokenId.toString());
            String encodedId = Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedId);

            String payload = String.join(":", encodedId, UUID.randomUUID().toString());
            byte[] encryptedPayloadBytes = encrypt(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedPayloadBytes);

            byte[] hmac = hmacHashByteData(encryptedPayloadBytes);
            String encodedHmac = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);

            return String.join(".", encodedPayload, encodedHmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> parseSecurityToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2)
            return Map.of();

        try {
            byte[] encryptedPayload = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[1]);

            byte[] expectedHmac = hmacHashByteData(encryptedPayload);
            if (!MessageDigest.isEqual(signatureBytes, expectedHmac))
                return Map.of();

            String decryptedPayload = decrypt(encryptedPayload);
            String[] payloadParts = decryptedPayload.split(":");
            if (payloadParts.length != 2)
                return Map.of();

            return Map.of("tokenId", decrypt(Base64.getUrlDecoder().decode(payloadParts[0])));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private byte[] hmacHashByteData(byte[] data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(hasherSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);

            return sha256Hmac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String encryptToBase64(String text) {
        return Base64.getEncoder().encodeToString(encrypt(text));
    }

    public String decryptFromBase64(String base64Text) {
        return decrypt(Base64.getDecoder().decode(base64Text));
    }

    private byte[] encrypt(String text) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getAesKey());
            return cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decrypt(byte[] encryptedBytes) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getAesKey());
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    //<editor-fold desc="OAuth State Handling">
    public String createOAuthState(String userEmail) {
        String payload = userEmail + ":" + System.currentTimeMillis();
        return encryptToBase64(payload);
    }

    public Map<String,String> parseOAuthState(String state) {
        String decryptedPayload = decryptFromBase64(state);
        String[] parts = decryptedPayload.split(":");
        return Map.of("userEmail", parts[0]);
    }

    // Generate a cryptographically random string of the desired length
    private static String generateRandomString(int byteLength) {
        byte[] code = new byte[byteLength];
        secureRandom.nextBytes(code);
        // Base64 URL-safe encoding without padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }

    /**
     * Generate the "state" parameter
     */
    public static String generateState() {
        // 16–32 bytes is enough, here we use 24 for ~32 chars output
        return generateRandomString(24);
    }

    /**
     * Generate the "code_verifier" parameter
     */
    public static String generateCodeVerifier() {
        // RFC 7636 requires 43–128 characters
        // 32 bytes will give ~43 chars Base64URL string
        return generateRandomString(32);
    }

    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
    //</editor-fold>
}
