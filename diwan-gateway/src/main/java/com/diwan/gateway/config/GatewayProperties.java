package com.diwan.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Tuning for routing, resilience and rate limiting (all keys under {@code diwan.gateway}). */
@ConfigurationProperties(prefix = "diwan.gateway")
public class GatewayProperties {

    /** How many trusted reverse proxies (nginx) sit in front; the client IP is taken from X-Forwarded-For accordingly. */
    private int trustedProxies = 1;
    private final Routes routes = new Routes();
    private final Resilience resilience = new Resilience();
    private final RateLimit rateLimit = new RateLimit();

    public int getTrustedProxies() { return trustedProxies; }
    public void setTrustedProxies(int trustedProxies) { this.trustedProxies = trustedProxies; }
    public Routes getRoutes() { return routes; }
    public Resilience getResilience() { return resilience; }
    public RateLimit getRateLimit() { return rateLimit; }

    public static class Routes {
        /** Every gateway instance re-reads service_routes at this interval and reloads if something changed. */
        private long refreshIntervalMs = 30_000;
        public long getRefreshIntervalMs() { return refreshIntervalMs; }
        public void setRefreshIntervalMs(long v) { this.refreshIntervalMs = v; }
    }

    public static class Resilience {
        /** Retries (GET only) on connection errors and 502/503/504 from the service. 0 disables. */
        private int retries = 2;
        private int connectTimeoutMs = 2_000;
        /** Used when a route row has no positive timeout_ms. */
        private int defaultResponseTimeoutMs = 10_000;
        public int getRetries() { return retries; }
        public void setRetries(int v) { this.retries = v; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int v) { this.connectTimeoutMs = v; }
        public int getDefaultResponseTimeoutMs() { return defaultResponseTimeoutMs; }
        public void setDefaultResponseTimeoutMs(int v) { this.defaultResponseTimeoutMs = v; }
    }

    public static class RateLimit {
        private boolean enabled = true;
        /** Per authenticated user (client IP when there is no user). */
        private final Bucket user = new Bucket(20, 40, 1);
        /** Strict per-IP limit for credential endpoints (brute-force protection): ~1 request / 12 s after a burst of 5. */
        private final Bucket auth = new Bucket(1, 60, 12);
        private List<String> authPaths = new ArrayList<>(List.of("/api/users/login", "/api/users/register"));

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Bucket getUser() { return user; }
        public Bucket getAuth() { return auth; }
        public List<String> getAuthPaths() { return authPaths; }
        public void setAuthPaths(List<String> authPaths) { this.authPaths = authPaths; }
    }

    /** Token bucket: {@code replenishRate} tokens/second, up to {@code burstCapacity}; a request costs {@code requestedTokens}. */
    public static class Bucket {
        private int replenishRate;
        private int burstCapacity;
        private int requestedTokens;

        public Bucket() {}
        public Bucket(int replenishRate, int burstCapacity, int requestedTokens) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
            this.requestedTokens = requestedTokens;
        }
        public int getReplenishRate() { return replenishRate; }
        public void setReplenishRate(int v) { this.replenishRate = v; }
        public int getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(int v) { this.burstCapacity = v; }
        public int getRequestedTokens() { return requestedTokens; }
        public void setRequestedTokens(int v) { this.requestedTokens = v; }
    }
}
