package com.diwan.gateway.config;

import com.diwan.gateway.repository.ServiceRouteRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class DynamicRouteConfig {

    /** Routes live in the service_routes table; see {@link RouteRefresher} for how changes are picked up. */
    @Bean
    public RouteDefinitionLocator dbRouteDefinitionLocator(ServiceRouteRepository routeRepository, GatewayProperties props) {
        RouteDefinitionFactory factory = new RouteDefinitionFactory(props);
        return () -> Mono.fromCallable(routeRepository::findByActiveTrue)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(rows -> Flux.fromIterable(factory.build(rows)));
    }

    @Bean
    public ClientIp clientIp(GatewayProperties props) {
        return new ClientIp(props.getTrustedProxies());
    }

    /** Per user (set by JwtAuthFilter); anonymous callers are limited per client IP. */
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver(ClientIp clientIp) {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? "u:" + userId : "ip:" + clientIp.of(exchange));
        };
    }

    @Bean
    public KeyResolver ipKeyResolver(ClientIp clientIp) {
        return exchange -> Mono.just("ip:" + clientIp.of(exchange));
    }

    /**
     * Circuit breaker defaults for every service. The per-route response timeout (service_routes.timeout_ms)
     * is what limits slow calls, so the breaker's own time limiter is only a safety net.
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerDefaults() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(60)).build())
                .build());
    }
}
