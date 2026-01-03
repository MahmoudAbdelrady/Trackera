package com.mdevs.trackera.utils;

import com.mdevs.trackera.service.UserInvalidTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final UserInvalidTokenService userInvalidTokenService;

    @Value("${trackera.tokens.access}")
    private String accessTokenSecretKey;

    @Value("${trackera.tokens.refresh}")
    private String refreshTokenSecretKey;

    public String getToken(HttpServletRequest request, boolean isAccessToken) {
        String token = CookieHelper.extractCookieValue(request, isAccessToken ? CookieHelper.ACCESS_TOKEN_COOKIE_NAME : CookieHelper.REFRESH_TOKEN_COOKIE_NAME);
        return !StringUtils.isEmpty(token) ? token : null;
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
        if (userInvalidTokenService.isTokenInvalid(uuid, token, isAccessToken)) {
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
}
