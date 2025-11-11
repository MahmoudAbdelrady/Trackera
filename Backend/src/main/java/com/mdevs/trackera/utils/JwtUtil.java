package com.mdevs.trackera.utils;

import com.mdevs.trackera.entity.UserInvalidToken;
import com.mdevs.trackera.repository.UserInvalidTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private final UserInvalidTokenRepository userInvalidTokenRepository;

    private final CryptoUtil cryptoUtil;

    @Value("${trackera.tokens.access}")
    private String accessTokenSecretKey;

    @Value("${trackera.tokens.refresh}")
    private String refreshTokenSecretKey;

    public JwtUtil(UserInvalidTokenRepository userInvalidTokenRepository, CryptoUtil cryptoUtil) {
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.cryptoUtil = cryptoUtil;
    }

    public String generateToken(String userUuid, boolean isAccessToken) {
        Instant now = Instant.now();
        Instant expiration = now.plus(15, isAccessToken ? ChronoUnit.MINUTES : ChronoUnit.DAYS);
        String tokenSecretKey = isAccessToken ? accessTokenSecretKey : refreshTokenSecretKey;
        return Jwts.builder()
                .subject("Token")
                .issuer("Trackera")
                .claim("id", userUuid)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(tokenSecretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Claims validateAndGetTokenPayload(String token, boolean isAccessToken) {
        Claims claims = getTokenPayload(token, isAccessToken);
        String uuid = claims.get("id", String.class);
        if (isTokenInvalid(uuid, token, isAccessToken)) {
            throw new SecurityException("Session expired.");
        }
        return claims;
    }

    public Claims getTokenPayload(String token, boolean isAccessToken) {
        try {
            String tokenSecretKey = isAccessToken ? accessTokenSecretKey : refreshTokenSecretKey;
            SecretKey secretKey = Keys.hmacShaKeyFor(tokenSecretKey.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new SecurityException("Invalid or expired token");
        }
    }

    public boolean isTokenInvalid(String uuid, String token, boolean isAccessToken) {
        long maxId = 0;
        PageRequest pageRequest = PageRequest.of(0, 100);
        List<UserInvalidToken> userInvalidTokens;
        boolean isInvalid = false;
        do {
            userInvalidTokens = userInvalidTokenRepository.findAllByUserAndTokenTypeOrderById(uuid, isAccessToken, maxId, pageRequest);
            if (!userInvalidTokens.isEmpty()) {
                isInvalid = userInvalidTokens.stream().anyMatch(it -> cryptoUtil.isMatch(token, it.getToken(), false));
                maxId = userInvalidTokens.getLast().getId();
            }
        } while (!userInvalidTokens.isEmpty() && !isInvalid);

        return isInvalid;
    }
}
