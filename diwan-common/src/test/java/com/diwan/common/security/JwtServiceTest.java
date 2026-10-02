package com.diwan.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";
    private final JwtService jwt = new JwtService(SECRET, 60_000);

    @Test
    void roundTripKeepsClaims() {
        String token = jwt.generateToken(7L, "u@example.com");
        Claims c = jwt.getClaims(token);
        assertEquals("u@example.com", c.getSubject());
        assertEquals(7L, jwt.extractUserId(token));
        assertNull(c.get("role", String.class));
        assertEquals(c.getId(), jwt.getTokenId(token));
        assertTrue(jwt.getRemainingTtl(token).toMillis() > 0);
    }

    @Test
    void roleIsIncludedWhenGiven() {
        assertEquals("ADMIN", jwt.getClaims(jwt.generateToken(1L, "a@b.c", "ADMIN")).get("role", String.class));
    }

    @Test
    void tokensAreUnique() {
        assertNotEquals(jwt.generateToken(7L, "u@example.com"), jwt.generateToken(7L, "u@example.com"));
    }

    @Test
    void rejectsTamperedForeignAndExpiredTokens() {
        String token = jwt.generateToken(7L, "u@example.com");
        assertFalse(jwt.isValid(token + "x"));
        assertFalse(jwt.isValid("not-a-jwt"));
        assertFalse(new JwtService(SECRET + "other", 60_000).isValid(token));
        JwtService expired = new JwtService(SECRET, -1_000);
        assertFalse(jwt.isValid(expired.generateToken(7L, "u@example.com")));
    }
}
