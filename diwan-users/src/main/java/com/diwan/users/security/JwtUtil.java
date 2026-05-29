package com.diwan.users.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${diwan.jwt.secret}")
    private String secret;

    @Value("${diwan.jwt.expiration}")
    private long expiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(exp)
            .signWith(key())
            .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public Duration getRemainingTtl(String token) {
        Date exp = getClaims(token).getExpiration();
        long remaining = exp.getTime() - System.currentTimeMillis();
        return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
    }
}
