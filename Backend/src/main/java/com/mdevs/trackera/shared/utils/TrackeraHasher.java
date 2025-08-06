package com.mdevs.trackera.shared.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class TrackeraHasher {

    @Value("${trackera.hasher_secret_key}")
    private String hasherSecretKey;

    @Value("${trackera.hasher.encryption_key}")
    private String encryptionSecretKey;

    private final static String ENCRYPTION_ALGORITHM = "AES";

    private SecretKey getAesKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(encryptionSecretKey), "AES");
    }

    public String hash(String text, boolean isUrl) {
        try {
            byte[] encryptedData = encrypt(text);
            Base64.Encoder encoder = isUrl ? Base64.getUrlEncoder() : Base64.getEncoder();
            return encoder.withoutPadding().encodeToString(hashByteData(encryptedData));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMatch(String text, String hash, boolean isUrl) {
        try {
            byte[] encryptedData = encrypt(text);
            byte[] expectedHash = isUrl ? Base64.getUrlDecoder().decode(hash) : Base64.getDecoder().decode(hash);
            byte[] actualHash = hashByteData(encryptedData);
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

            byte[] hmac = hashByteData(encryptedPayloadBytes);
            String encodedHmac = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);

            return String.join(".", encodedPayload, encodedHmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> parseSecurityToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2)
            return Collections.emptyMap();

        try {
            byte[] encryptedPayload = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[1]);

            byte[] expectedHmac = hashByteData(encryptedPayload);
            if (!MessageDigest.isEqual(signatureBytes, expectedHmac))
                return Collections.emptyMap();

            String decryptedPayload = decrypt(encryptedPayload);
            String[] payloadParts = decryptedPayload.split(":");
            if (payloadParts.length != 2)
                return Collections.emptyMap();

            return Map.of("tokenId", decrypt(Base64.getUrlDecoder().decode(payloadParts[0])));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private byte[] hashByteData(byte[] data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(hasherSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);

            return sha256Hmac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
