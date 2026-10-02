package com.diwan.gateway.config;

import com.diwan.gateway.entity.ServiceRoute;
import com.diwan.gateway.repository.ServiceRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The gateway caches its routes and only rebuilds them on a RefreshRoutesEvent. Changes made through
 * {@code RouteService} refresh the local instance immediately; this poller makes every OTHER instance (and
 * manual edits straight in the database) follow within {@code diwan.gateway.routes.refresh-interval-ms}.
 */
@Component
public class RouteRefresher {

    private static final Logger log = LoggerFactory.getLogger(RouteRefresher.class);

    private final ServiceRouteRepository repository;
    private final ApplicationEventPublisher publisher;
    private volatile Integer lastFingerprint;

    public RouteRefresher(ServiceRouteRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${diwan.gateway.routes.refresh-interval-ms:30000}",
               initialDelayString = "${diwan.gateway.routes.refresh-interval-ms:30000}")
    public void refreshIfChanged() {
        try {
            int current = fingerprint(repository.findAll());
            Integer previous = lastFingerprint;
            lastFingerprint = current;
            if (previous != null && previous != current) {
                log.info("[Routes] service_routes changed in the database, reloading routes");
                publisher.publishEvent(new RefreshRoutesEvent(this));
            }
        } catch (Exception e) {
            log.warn("[Routes] could not check service_routes for changes: {}", e.getMessage());
        }
    }

    /** Remember the current state, e.g. right after this instance changed routes itself. */
    public void markCurrent() {
        try {
            lastFingerprint = fingerprint(repository.findAll());
        } catch (Exception e) {
            lastFingerprint = null;
        }
    }

    static int fingerprint(List<ServiceRoute> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(ServiceRoute::getServiceName))
                .map(r -> Objects.hash(r.getServiceName(), r.getBaseUrl(), r.getPathPrefix(), r.isActive(), r.getTimeoutMs()))
                .toList().hashCode();
    }
}
