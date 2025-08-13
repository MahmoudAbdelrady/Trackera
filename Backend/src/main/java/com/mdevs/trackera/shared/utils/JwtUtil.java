package com.mdevs.trackera.shared.utils;

import com.mdevs.trackera.entity.UserInvalidToken;
import com.mdevs.trackera.repository.UserInvalidTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final TrackeraHasher trackeraHasher;

    @Value("${trackera.tokens.access}")
    private String accessTokenSecretKey;

    @Value("${trackera.tokens.refresh}")
    private String refreshTokenSecretKey;

    @Autowired
    public JwtUtil(UserInvalidTokenRepository userInvalidTokenRepository, TrackeraHasher trackeraHasher) {
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.trackeraHasher = trackeraHasher;
    }

    public String generateToken(String userEmail, boolean isAccessToken) {
        Instant now = Instant.now();
        Instant expiration = now.plus(15, isAccessToken ? ChronoUnit.MINUTES : ChronoUnit.DAYS);
        String tokenSecretKey = isAccessToken ? accessTokenSecretKey : refreshTokenSecretKey;
        return Jwts.builder()
                .subject("Token")
                .issuer("Trackera")
                .claim("email", userEmail)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(tokenSecretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
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

    public boolean isTokenInvalid(String userEmail, String token, boolean isAccessToken) {
        long maxId = 0;
        PageRequest pageRequest = PageRequest.of(0, 100);
        List<UserInvalidToken> userInvalidTokens;
        boolean isInvalid = false;
        do {
            userInvalidTokens = userInvalidTokenRepository.findAllByUserAndTokenTypeOrderById(userEmail, isAccessToken, maxId, pageRequest);
            if (!userInvalidTokens.isEmpty()) {
                isInvalid = userInvalidTokens.stream().anyMatch(it -> trackeraHasher.isMatch(token, it.getToken(), false));
                maxId = userInvalidTokens.getLast().getId();
            }
        } while (!userInvalidTokens.isEmpty() && !isInvalid);

        return isInvalid;
    }
}
