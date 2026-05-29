package com.diwan.gateway.config;

import com.diwan.gateway.entity.ServiceRoute;
import com.diwan.gateway.repository.ServiceRouteRepository;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Configuration
public class DynamicRouteConfig {

    private final ServiceRouteRepository routeRepository;

    public DynamicRouteConfig(ServiceRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Bean
    public RouteDefinitionLocator dbRouteDefinitionLocator() {
        return () -> Mono.fromCallable(() -> routeRepository.findByActiveTrue())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(this::toRouteDefinition);
    }

    private RouteDefinition toRouteDefinition(ServiceRoute sr) {
        RouteDefinition def = new RouteDefinition();
        def.setId(sr.getServiceName() + "-" + UUID.randomUUID());
        def.setUri(URI.create(sr.getBaseUrl()));

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", sr.getPathPrefix() + "/**");
        def.setPredicates(List.of(predicate));

        def.setFilters(List.of());

        return def;
    }
}