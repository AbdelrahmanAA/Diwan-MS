package com.voicetransactions.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 256-bit secret — in production load from env variable
    private static final String SECRET =
        "VoiceApp_SecretKey_Must_Be_32_Chars_Min!";
    private static final long EXPIRY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    /** Generate a JWT containing userId + email */
    public String generateToken(Long userId, String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Extract email (subject) from token */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extract userId from token */
    public Long extractUserId(String token) {
        Object id = parseClaims(token).get("userId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        return (Long) id;
    }

    /** Validate token — returns true if valid and not expired */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
