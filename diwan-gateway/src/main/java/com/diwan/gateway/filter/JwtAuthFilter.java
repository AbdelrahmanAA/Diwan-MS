package com.diwan.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@Component
public class JwtAuthFilter implements WebFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/users/login",
        "/api/users/register",
        "/api/features",
        "/actuator/health",
        "/h2-console"
    );

    public JwtAuthFilter(JwtUtil jwtUtil, StringRedisTemplate redis) {
        this.jwtUtil = jwtUtil;
        this.redis   = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // 1. Validate JWT signature + expiry
        if (!jwtUtil.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 2. Check token blacklist (logout revocation)
        return Mono.fromCallable(() -> {
            String jti = jwtUtil.getTokenId(token);
            return Boolean.TRUE.equals(redis.hasKey("blacklist:" + jti));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(isBlacklisted -> {
            if (isBlacklisted) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            // 3. Inject user context into request headers
            try {
                var claims = jwtUtil.getClaims(token);
                Long   userId = claims.get("userId", Long.class);
                String role   = claims.get("role",   String.class);
                if (role == null) role = "USER";
                final String finalRole = role;
                ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                        .header("X-User-Id",           String.valueOf(userId))
                        .header("X-User-Email",         claims.getSubject())
                        .header("X-User-Role",          finalRole)
                        .header("X-Gateway-Validated", "true"))
                    .build();
                return chain.filter(mutated);
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        });
    }

    @Override
    public int getOrder() { return -1; }
}