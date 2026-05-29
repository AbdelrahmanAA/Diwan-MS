package com.diwan.gateway.service;

import com.diwan.gateway.entity.ServiceRoute;
import com.diwan.gateway.repository.ServiceRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {
    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private final ServiceRouteRepository repo;
    private final FeaturesCacheService cacheService;
    public RouteService(ServiceRouteRepository repo, FeaturesCacheService cacheService) {
        this.repo = repo; this.cacheService = cacheService;
    }
    public List<ServiceRoute> getActiveRoutes() { return repo.findByActiveTrue(); }
    public Optional<ServiceRoute> findByName(String name) { return repo.findByServiceNameAndActiveTrue(name); }
    public ServiceRoute save(ServiceRoute route) {
        ServiceRoute saved = repo.save(route);
        cacheService.refreshCache();
        return saved;
    }
    public void deactivate(String name) {
        repo.findByServiceNameAndActiveTrue(name).ifPresent(r -> {
            r.setActive(false); repo.save(r);
            cacheService.refreshCache();
        });
    }
    public boolean seedIfAbsent(String name, String baseUrl, String pathPrefix, String description) {
        if (repo.findByServiceNameAndActiveTrue(name).isEmpty()) {
            ServiceRoute r = new ServiceRoute();
            r.setServiceName(name); r.setBaseUrl(baseUrl);
            r.setPathPrefix(pathPrefix); r.setDescription(description); r.setActive(true);
            repo.save(r);
            return true;
        }
        return false;
    }
}