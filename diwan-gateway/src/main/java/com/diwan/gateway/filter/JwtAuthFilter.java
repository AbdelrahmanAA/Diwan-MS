package com.diwan.gateway.filter;

import com.diwan.common.security.GatewaySigner;
import com.diwan.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Single entry point for authentication:
 * 1. strips every client-supplied identity header (on public paths too),
 * 2. validates the JWT and the logout blacklist,
 * 3. enforces ADMIN on admin paths,
 * 4. forwards the identity as headers signed with {@link GatewaySigner}, which downstream
 *    services verify (they no longer trust a bare "X-Gateway-Validated" flag).
 */
@Component
public class JwtAuthFilter implements WebFilter, Ordered {

    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final GatewaySigner signer;
    private final List<String> adminPaths;
    private final int managementPort;

    // Matched on a "/" boundary. The Alexa endpoint authenticates itself in diwan-smarthome
    // (lambda shared secret + user JWT), so the Lambda can call it through the gateway.
    /** Reading the dashboard cards is public; any other method or sub-path under /api/features is admin-only. */
    private static boolean isPublicFeaturesRead(ServerWebExchange exchange, String path) {
        return HttpMethod.GET.equals(exchange.getRequest().getMethod())
                && (path.equals("/api/features") || path.equals("/api/features/"));
    }

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/users/login",
        "/api/users/register",
        "/actuator/health",
        "/api/smart-home/alexa"
    );

    public JwtAuthFilter(JwtService jwtService, StringRedisTemplate redis, GatewaySigner signer,
                         @Value("${diwan.gateway.admin-paths:/admin,/api/logs,/api/features,/actuator}") List<String> adminPaths,
                         @Value("${management.server.port:-1}") int managementPort) {
        this.jwtService = jwtService;
        this.redis   = redis;
        this.signer  = signer;
        this.adminPaths = adminPaths;
        this.managementPort = managementPort;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange original, WebFilterChain chain) {
        // 1. Never trust identity headers coming from outside
        ServerWebExchange exchange = original.mutate()
            .request(r -> r.headers(h -> GatewaySigner.TRUSTED_HEADERS.forEach(h::remove)))
            .build();

        String path = exchange.getRequest().getURI().getPath();
        if (matchesAny(PUBLIC_PATHS, path) || isPublicFeaturesRead(exchange, path)
                || ManagementPort.isManagementRequest(exchange, managementPort)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 2a. Validate JWT signature + expiry
        if (!jwtService.isValid(token)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        // 2b. Check token blacklist (logout revocation)
        return Mono.fromCallable(() -> {
            String jti = jwtService.getTokenId(token);
            return Boolean.TRUE.equals(redis.hasKey("blacklist:" + jti));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(isBlacklisted -> {
            if (isBlacklisted) {
                return reject(exchange, HttpStatus.UNAUTHORIZED);
            }
            try {
                var claims = jwtService.getClaims(token);
                Long   userId = claims.get("userId", Long.class);
                String role   = claims.get("role",   String.class);
                if (userId == null) return reject(exchange, HttpStatus.UNAUTHORIZED);
                if (role == null) role = "USER";

                // 3. Admin-only paths
                if (matchesAny(adminPaths, path) && !"ADMIN".equals(role)) {
                    return reject(exchange, HttpStatus.FORBIDDEN);
                }

                // 4. Signed identity for downstream services
                String uid   = String.valueOf(userId);
                String email = claims.getSubject();
                long   ts    = System.currentTimeMillis();
                String sig   = signer.sign(uid, email, role, ts);
                final String finalRole = role;
                ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.set(GatewaySigner.HDR_USER_ID,   uid);
                        h.set(GatewaySigner.HDR_EMAIL,     email);
                        h.set(GatewaySigner.HDR_ROLE,      finalRole);
                        h.set(GatewaySigner.HDR_TIMESTAMP, String.valueOf(ts));
                        h.set(GatewaySigner.HDR_SIGNATURE, sig);
                    }))
                    .build();
                return chain.filter(mutated);
            } catch (Exception e) {
                return reject(exchange, HttpStatus.UNAUTHORIZED);
            }
        });
    }

    private static boolean matchesAny(List<String> prefixes, String path) {
        for (String p : prefixes) {
            String prefix = p.trim();
            if (prefix.isEmpty()) continue;
            if (path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")) return true;
        }
        return false;
    }

    private static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -1; }
}
