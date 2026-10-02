package com.diwan.gateway.config;

import com.diwan.gateway.entity.ServiceRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns service_routes rows into gateway routes. Every route gets, outermost first:
 * <ol>
 *   <li>a Redis-backed rate limit (per user, or per IP when anonymous),</li>
 *   <li>a circuit breaker with a 503/504 fallback (trips on connection errors, timeouts and 502/503/504),</li>
 *   <li>retries for GET requests only (never repeat a POST/PATCH/DELETE), on connection errors and 502/503.</li>
 * </ol>
 * Credential endpoints (login/register) additionally get their own strict per-IP limit.
 */
public class RouteDefinitionFactory {

    private static final Logger log = LoggerFactory.getLogger(RouteDefinitionFactory.class);

    private final GatewayProperties props;

    public RouteDefinitionFactory(GatewayProperties props) {
        this.props = props;
    }

    public List<RouteDefinition> build(List<ServiceRoute> rows) {
        List<ServiceRoute> valid = new ArrayList<>();
        for (ServiceRoute row : rows) {
            var problem = RouteValidator.validate(row);
            if (problem.isPresent()) {
                log.error("[Routes] Ignoring invalid route '{}': {}", row.getServiceName(), problem.get());
            } else {
                valid.add(row);
            }
        }

        List<RouteDefinition> defs = new ArrayList<>();
        for (ServiceRoute route : valid) {
            defs.add(serviceRoute(route));
        }
        if (props.getRateLimit().isEnabled()) {
            int n = 0;
            for (String path : props.getRateLimit().getAuthPaths()) {
                var owner = valid.stream()
                        .filter(r -> path.equals(r.getPathPrefix()) || path.startsWith(r.getPathPrefix() + "/"))
                        .max(Comparator.comparingInt(r -> r.getPathPrefix().length()));
                if (owner.isPresent()) {
                    defs.add(authRoute(owner.get(), path, n++));
                }
            }
        }
        return defs;
    }

    private RouteDefinition serviceRoute(ServiceRoute route) {
        RouteDefinition def = base(route, route.getServiceName(), route.getPathPrefix() + "/**");
        List<FilterDefinition> filters = new ArrayList<>();
        if (props.getRateLimit().isEnabled()) {
            filters.add(rateLimiter("userOrIpKeyResolver", props.getRateLimit().getUser()));
        }
        filters.add(circuitBreaker(route.getServiceName()));
        if (props.getResilience().getRetries() > 0) {
            filters.add(retry());
        }
        def.setFilters(filters);
        return def;
    }

    /** Evaluated before the service route (lower order = earlier), same target, stricter limit. */
    private RouteDefinition authRoute(ServiceRoute route, String path, int index) {
        RouteDefinition def = base(route, route.getServiceName() + "-auth-" + index, path);
        def.setOrder(-10);
        def.setFilters(List.of(
                rateLimiter("ipKeyResolver", props.getRateLimit().getAuth()),
                circuitBreaker(route.getServiceName())));
        return def;
    }

    private RouteDefinition base(ServiceRoute route, String id, String pathPattern) {
        RouteDefinition def = new RouteDefinition();
        def.setId(id);
        def.setUri(URI.create(route.getBaseUrl()));
        PredicateDefinition path = new PredicateDefinition("Path=" + pathPattern);
        def.setPredicates(List.of(path));
        int timeout = route.getTimeoutMs() > 0 ? route.getTimeoutMs() : props.getResilience().getDefaultResponseTimeoutMs();
        def.setMetadata(Map.of(
                "response-timeout", timeout,
                "connect-timeout", props.getResilience().getConnectTimeoutMs()));
        return def;
    }

    private static FilterDefinition rateLimiter(String keyResolverBean, GatewayProperties.Bucket bucket) {
        FilterDefinition f = new FilterDefinition();
        f.setName("RequestRateLimiter");
        f.addArg("key-resolver", "#{@" + keyResolverBean + "}");
        f.addArg("redis-rate-limiter.replenishRate", String.valueOf(bucket.getReplenishRate()));
        f.addArg("redis-rate-limiter.burstCapacity", String.valueOf(bucket.getBurstCapacity()));
        f.addArg("redis-rate-limiter.requestedTokens", String.valueOf(bucket.getRequestedTokens()));
        return f;
    }

    private static FilterDefinition circuitBreaker(String serviceName) {
        FilterDefinition f = new FilterDefinition();
        f.setName("CircuitBreaker");
        f.addArg("name", serviceName);
        f.addArg("fallbackUri", "forward:/fallback/" + serviceName);
        f.addArg("statusCodes", "502,503,504");
        return f;
    }

    private FilterDefinition retry() {
        FilterDefinition f = new FilterDefinition();
        f.setName("Retry");
        f.addArg("retries", String.valueOf(props.getResilience().getRetries()));
        f.addArg("methods", "GET");
        // Not on timeouts (a slow call would take retries x timeout) and not on 504
        f.addArg("statuses", "BAD_GATEWAY,SERVICE_UNAVAILABLE");
        f.addArg("exceptions", "java.io.IOException");
        f.addArg("backoff.firstBackoff", "100ms");
        f.addArg("backoff.maxBackoff", "1s");
        f.addArg("backoff.factor", "2");
        return f;
    }
}
