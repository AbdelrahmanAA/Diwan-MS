package com.diwan.gateway.filter;

import com.diwan.common.security.GatewaySigner;
import com.diwan.common.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final WebFilterChain chain = ex -> {
        forwarded.set(ex);
        return Mono.empty();
    };

    private JwtAuthFilter filter() {
        when(redis.hasKey(anyString())).thenReturn(false);
        return new JwtAuthFilter(new JwtService(SECRET, 60_000), redis, GatewaySigner.fromSecret(SECRET), List.of("/admin", "/api/logs", "/actuator"), 18111);
    }

    private String token(Long userId, String role) {
        var b = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("u@example.com")
                .claim("userId", userId)
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (role != null) b.claim("role", role);
        return b.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void overwritesForgedIdentityHeadersWithSignedOnes() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/transactions")
                .header("Authorization", "Bearer " + token(7L, null))
                .header("X-User-Id", "999")
                .header("X-User-Role", "ADMIN")
                .header("X-Gateway-Validated", "true")
                .build());
        filter().filter(ex, chain).block();

        var h = forwarded.get().getRequest().getHeaders();
        assertEquals(List.of("7"), h.get("X-User-Id"));
        assertEquals(List.of("USER"), h.get("X-User-Role"));
        assertNull(h.getFirst("X-Gateway-Validated"));
        GatewaySigner signer = GatewaySigner.fromSecret(SECRET);
        assertTrue(signer.verify("7", "u@example.com", "USER",
                Long.parseLong(h.getFirst("X-Gateway-Timestamp")), h.getFirst("X-Gateway-Signature")));
    }

    @Test
    void publicPathsStillDropForgedHeaders() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.post("/api/users/login")
                .header("X-User-Id", "999").header("X-Gateway-Signature", "x").build());
        filter().filter(ex, chain).block();
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"));
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-Gateway-Signature"));
    }

    @Test
    void rejectsMissingOrInvalidToken() {
        MockServerWebExchange none = MockServerWebExchange.from(MockServerHttpRequest.get("/api/medical/records").build());
        filter().filter(none, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, none.getResponse().getStatusCode());

        MockServerWebExchange bad = MockServerWebExchange.from(MockServerHttpRequest.get("/api/medical/records")
                .header("Authorization", "Bearer not-a-jwt").build());
        filter().filter(bad, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, bad.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void adminPathsRequireAdminRole() {
        for (String path : List.of("/admin/routes", "/api/logs/errors", "/actuator/gateway/routes")) {
            MockServerWebExchange user = MockServerWebExchange.from(MockServerHttpRequest.get(path)
                    .header("Authorization", "Bearer " + token(7L, null)).build());
            filter().filter(user, chain).block();
            assertEquals(HttpStatus.FORBIDDEN, user.getResponse().getStatusCode(), path);
        }
        assertNull(forwarded.get());

        MockServerWebExchange admin = MockServerWebExchange.from(MockServerHttpRequest.get("/admin/routes")
                .header("Authorization", "Bearer " + token(1L, "ADMIN")).build());
        filter().filter(admin, chain).block();
        assertEquals(List.of("ADMIN"), forwarded.get().getRequest().getHeaders().get("X-User-Role"));
    }

    @Test
    void managementPortNeedsNoTokenButTheAppPortStillDoes() {
        MockServerWebExchange mgmt = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/prometheus")
                .localAddress(new java.net.InetSocketAddress("localhost", 18111)).build());
        filter().filter(mgmt, chain).block();
        assertNotNull(forwarded.get()); // reached the handler without a token

        forwarded.set(null);
        MockServerWebExchange app = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/prometheus")
                .localAddress(new java.net.InetSocketAddress("localhost", 8080)).build());
        filter().filter(app, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, app.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void blacklistedTokenIsRejected() {
        JwtAuthFilter f = filter();
        when(redis.hasKey(anyString())).thenReturn(true);
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/transactions")
                .header("Authorization", "Bearer " + token(7L, null)).build());
        f.filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }
}
