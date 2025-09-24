package com.mdevs.trackera.shared.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Component
public class TrackeraHasher {

    @Value("${trackera.hasher_secret_key}")
    private String hasherSecretKey;

    @Value("${trackera.hasher.encryption_key}")
    private String encryptionSecretKey;

    private final static String ENCRYPTION_ALGORITHM = "AES";

    private static final SecureRandom secureRandom = new SecureRandom();

    private SecretKey getAesKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(encryptionSecretKey), "AES");
    }

    public String hash(String text, boolean isUrl) {
        try {
            byte[] encryptedData = encrypt(text);
            return encodeToBase64(hmacHashByteData(encryptedData), isUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMatch(String text, String hash, boolean isUrl) {
        try {
            byte[] encryptedData = encrypt(text);
            byte[] expectedHash = decodeFromBase64(hash, isUrl);
            byte[] actualHash = hmacHashByteData(encryptedData);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String hashForSecurityToken(Long secTokenId) {
        try {
            byte[] encryptedId = encrypt(secTokenId.toString());
            String encodedId = encodeToBase64(encryptedId, true);

            String payload = String.join(":", encodedId, UUID.randomUUID().toString());
            byte[] encryptedPayloadBytes = encrypt(payload);
            String encodedPayload = encodeToBase64(encryptedPayloadBytes, true);

            byte[] hmac = hmacHashByteData(encryptedPayloadBytes);
            String encodedHmac = encodeToBase64(hmac, true);

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
            byte[] encryptedPayload = decodeFromBase64(parts[0], true);
            byte[] signatureBytes = decodeFromBase64(parts[1], true);

            byte[] expectedHmac = hmacHashByteData(encryptedPayload);
            if (!MessageDigest.isEqual(signatureBytes, expectedHmac))
                return Map.of();

            String decryptedPayload = decrypt(encryptedPayload);
            String[] payloadParts = decryptedPayload.split(":");
            if (payloadParts.length != 2)
                return Map.of();

            return Map.of("tokenId", decrypt(decodeFromBase64(payloadParts[0], true)));
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

    public String encryptToBase64(String text, boolean isUrl) {
        return encodeToBase64(encrypt(text), isUrl);
    }

    public String decryptFromBase64(String base64Text, boolean isUrl) {
        return decrypt(decodeFromBase64(base64Text, isUrl));
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

    public String encodeToBase64(byte[] text, boolean isUrl) {
        Base64.Encoder encoder = isUrl ? Base64.getUrlEncoder() : Base64.getEncoder();
        return encoder.withoutPadding().encodeToString(text);
    }

    public byte[] decodeFromBase64(String base64Text, boolean isUrl) {
        Base64.Decoder decoder = isUrl ? Base64.getUrlDecoder() : Base64.getDecoder();
        return decoder.decode(base64Text);
    }

    public String generateRandomString(int byteLength) {
        byte[] code = new byte[byteLength];
        secureRandom.nextBytes(code);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }
}
