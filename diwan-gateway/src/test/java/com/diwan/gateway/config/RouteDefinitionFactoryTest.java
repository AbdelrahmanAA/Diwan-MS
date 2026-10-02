package com.diwan.gateway.config;

import com.diwan.gateway.entity.ServiceRoute;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDefinitionFactoryTest {

    private final GatewayProperties props = new GatewayProperties();
    private final RouteDefinitionFactory factory = new RouteDefinitionFactory(props);

    private static ServiceRoute route(String name, String url, String prefix, int timeoutMs) {
        ServiceRoute r = new ServiceRoute();
        r.setServiceName(name);
        r.setBaseUrl(url);
        r.setPathPrefix(prefix);
        r.setTimeoutMs(timeoutMs);
        return r;
    }

    private static List<String> filterNames(RouteDefinition d) {
        return d.getFilters().stream().map(FilterDefinition::getName).toList();
    }

    private static RouteDefinition byId(List<RouteDefinition> defs, String id) {
        return defs.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void serviceRouteGetsRateLimitBreakerAndRetryInThatOrder() {
        RouteDefinition d = byId(factory.build(List.of(route("medical", "http://medical:8080", "/api/medical", 7000))), "medical");
        assertEquals("http://medical:8080", d.getUri().toString());
        assertEquals(List.of("RequestRateLimiter", "CircuitBreaker", "Retry"), filterNames(d));
        assertEquals(7000, d.getMetadata().get("response-timeout"));
        assertEquals(2000, d.getMetadata().get("connect-timeout"));
    }

    @Test
    void retriesAreLimitedToGetRequests() {
        RouteDefinition d = byId(factory.build(List.of(route("medical", "http://medical:8080", "/api/medical", 5000))), "medical");
        FilterDefinition retry = d.getFilters().stream().filter(f -> f.getName().equals("Retry")).findFirst().orElseThrow();
        assertEquals("GET", retry.getArgs().get("methods"));
    }

    @Test
    void circuitBreakerFallsBackPerServiceAndOnlyCountsGatewayStyleErrors() {
        RouteDefinition d = byId(factory.build(List.of(route("medical", "http://medical:8080", "/api/medical", 5000))), "medical");
        FilterDefinition cb = d.getFilters().stream().filter(f -> f.getName().equals("CircuitBreaker")).findFirst().orElseThrow();
        assertEquals("forward:/fallback/medical", cb.getArgs().get("fallbackUri"));
        assertEquals("502,503,504", cb.getArgs().get("statusCodes"));
    }

    @Test
    void missingTimeoutFallsBackToTheDefault() {
        RouteDefinition d = byId(factory.build(List.of(route("medical", "http://medical:8080", "/api/medical", 0))), "medical");
        assertEquals(props.getResilience().getDefaultResponseTimeoutMs(), d.getMetadata().get("response-timeout"));
    }

    @Test
    void loginAndRegisterGetAStricterRouteThatIsMatchedFirst() {
        List<RouteDefinition> defs = factory.build(List.of(route("users", "http://users:8080", "/api/users", 5000)));
        RouteDefinition auth0 = byId(defs, "users-auth-0");
        RouteDefinition auth1 = byId(defs, "users-auth-1");
        assertEquals(-10, auth0.getOrder());
        assertEquals("Path=/api/users/login", auth0.getPredicates().get(0).getName() + "=" + auth0.getPredicates().get(0).getArgs().values().iterator().next());
        assertEquals("Path=/api/users/register", auth1.getPredicates().get(0).getName() + "=" + auth1.getPredicates().get(0).getArgs().values().iterator().next());
        FilterDefinition limiter = auth0.getFilters().get(0);
        assertEquals("#{@ipKeyResolver}", limiter.getArgs().get("key-resolver"));
        assertEquals("12", limiter.getArgs().get("redis-rate-limiter.requestedTokens"));
    }

    @Test
    void noAuthRouteWhenNoServiceOwnsThePath() {
        List<RouteDefinition> defs = factory.build(List.of(route("medical", "http://medical:8080", "/api/medical", 5000)));
        assertEquals(1, defs.size());
    }

    @Test
    void invalidRowsAreSkippedInsteadOfBreakingTheGateway() {
        List<RouteDefinition> defs = factory.build(List.of(
                route("portfolio", "http://portfolio:8080", "/api/v1/portfolios/**", 5000),
                route("broken", "not a url", "/api/broken", 5000),
                route("medical", "http://medical:8080", "/api/medical", 5000)));
        assertEquals(List.of("medical"), defs.stream().map(RouteDefinition::getId).toList());
    }

    @Test
    void rateLimitingAndRetriesCanBeSwitchedOff() {
        props.getRateLimit().setEnabled(false);
        props.getResilience().setRetries(0);
        List<RouteDefinition> defs = factory.build(List.of(route("users", "http://users:8080", "/api/users", 5000)));
        assertEquals(1, defs.size());
        assertEquals(List.of("CircuitBreaker"), filterNames(defs.get(0)));
        assertFalse(filterNames(defs.get(0)).contains("Retry"));
    }

    @Test
    void validatorRules() {
        assertTrue(RouteValidator.validate(route("a", "http://a:8080", "/api/v1/a-b", 1)).isEmpty());
        assertTrue(RouteValidator.validate(route("a", "http://a:8080", "/", 1)).isPresent());
        assertTrue(RouteValidator.validate(route("a", "http://a:8080", "/api/a/", 1)).isPresent());
        assertTrue(RouteValidator.validate(route("a", "http://a:8080", "/api/*", 1)).isPresent());
        assertTrue(RouteValidator.validate(route("a", "ftp://a", "/api/a", 1)).isPresent());
        assertTrue(RouteValidator.validate(route("a", "http://a:8080?x=1", "/api/a", 1)).isPresent());
        assertTrue(RouteValidator.validate(route("a", "http://a:8080", "/api/a", -1)).isPresent());
        assertTrue(RouteValidator.validate(route(" ", "http://a:8080", "/api/a", 1)).isPresent());
    }
}
