package com.diwan.gateway.controller;
import com.diwan.gateway.entity.ServiceRoute;
import com.diwan.gateway.service.FeaturesCacheService;
import com.diwan.gateway.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/routes")
public class RouteManagementController {
    private final RouteService routeService;
    private final FeaturesCacheService cacheService;
    public RouteManagementController(RouteService routeService, FeaturesCacheService cacheService) {
        this.routeService = routeService; this.cacheService = cacheService;
    }
    @GetMapping
    public ResponseEntity<List<ServiceRoute>> getAll() {
        return ResponseEntity.ok(routeService.getActiveRoutes());
    }
    @PostMapping
    public ResponseEntity<ServiceRoute> create(@RequestBody ServiceRoute route) {
        return ResponseEntity.ok(routeService.save(route));
    }
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshCache() {
        cacheService.refreshCache();
        return ResponseEntity.ok().build();
    }
}