package com.diwan.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/** Issues and parses the HS256 JWTs shared by every service (same {@code JWT_SECRET}). */
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(String secret, long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, null);
    }

    /** @param role optional; tokens without a role are treated as USER by the gateway */
    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis));
        if (role != null) builder.claim("role", role);
        return builder.signWith(key).compact();
    }

    /** @throws io.jsonwebtoken.JwtException if the token is malformed, tampered with or expired */
    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    /** The jti claim, unique per token; used for logout blacklisting. */
    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public Duration getRemainingTtl(String token) {
        long remaining = getClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
    }
}
