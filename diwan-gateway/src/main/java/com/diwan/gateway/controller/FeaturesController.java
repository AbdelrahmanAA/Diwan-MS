package com.diwan.gateway.controller;

import com.diwan.gateway.entity.AppFeature;
import com.diwan.gateway.service.AppFeatureService;
import com.diwan.gateway.service.FeaturesCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/features")
@CrossOrigin(origins = "*")
public class FeaturesController {

    private final FeaturesCacheService cacheService;
    private final AppFeatureService featureService;

    public FeaturesController(FeaturesCacheService cacheService, AppFeatureService featureService) {
        this.cacheService = cacheService;
        this.featureService = featureService;
    }

    // ── Public: mobile app reads this ───────────────────────────────────────

    @GetMapping
    public Mono<ResponseEntity<List<Map<String, Object>>>> getFeatures() {
        return Mono.fromCallable(() ->
                ResponseEntity.ok(cacheService.getFeatures())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    // ── Admin: manage features ───────────────────────────────────────────────

    @GetMapping("/all")
    public Mono<ResponseEntity<List<AppFeature>>> getAllFeatures() {
        return Mono.fromCallable(() ->
                ResponseEntity.ok(featureService.findAll())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<ResponseEntity<AppFeature>> createFeature(@RequestBody AppFeature feature) {
        return Mono.fromCallable(() ->
                ResponseEntity.ok(featureService.save(feature))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<AppFeature>> updateFeature(@PathVariable Long id,
                                                          @RequestBody AppFeature updates) {
        return Mono.fromCallable(() ->
                ResponseEntity.ok(featureService.update(id, updates))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @PatchMapping("/{id}/toggle")
    public Mono<ResponseEntity<AppFeature>> toggleFeature(@PathVariable Long id) {
        return Mono.fromCallable(() ->
                ResponseEntity.ok(featureService.toggleActive(id))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteFeature(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            featureService.delete(id);
            return ResponseEntity.ok("Feature deleted and cache refreshed");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<String>> refreshCache() {
        return Mono.fromCallable(() -> {
            cacheService.refreshCache();
            return ResponseEntity.ok("Features cache refreshed");
        }).subscribeOn(Schedulers.boundedElastic());
    }
}