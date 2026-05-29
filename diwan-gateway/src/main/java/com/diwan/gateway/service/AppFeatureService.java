package com.diwan.gateway.service;

import com.diwan.gateway.entity.AppFeature;
import com.diwan.gateway.repository.AppFeatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppFeatureService {

    private static final Logger log = LoggerFactory.getLogger(AppFeatureService.class);

    private final AppFeatureRepository repository;
    private final FeaturesCacheService cacheService;

    public AppFeatureService(AppFeatureRepository repository, FeaturesCacheService cacheService) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    // ── Read ────────────────────────────────────────────────────────────────

    public List<AppFeature> findAll() {
        return repository.findAll();
    }

    public List<AppFeature> findAllActive() {
        return repository.findByActiveTrueOrderBySortOrderAsc();
    }

    public Optional<AppFeature> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<AppFeature> findByName(String name) {
        return repository.findByName(name);
    }

    // ── Write (each one auto-invalidates Redis) ──────────────────────────────

    public AppFeature save(AppFeature feature) {
        AppFeature saved = repository.save(feature);
        cacheService.refreshCache();
        log.info("[AppFeatureService] Saved feature '{}' - Redis cache refreshed", saved.getName());
        return saved;
    }

    public AppFeature update(Long id, AppFeature updates) {
        AppFeature existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found: " + id));
        if (updates.getDisplayName() != null) existing.setDisplayName(updates.getDisplayName());
        if (updates.getIcon()        != null) existing.setIcon(updates.getIcon());
        if (updates.getColor()       != null) existing.setColor(updates.getColor());
        if (updates.getScreenName()  != null) existing.setScreenName(updates.getScreenName());
        if (updates.getPath()        != null) existing.setPath(updates.getPath());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        existing.setSortOrder(updates.getSortOrder());
        existing.setActive(updates.isActive());
        AppFeature saved = repository.save(existing);
        cacheService.refreshCache();
        log.info("[AppFeatureService] Updated feature '{}' - Redis cache refreshed", saved.getName());
        return saved;
    }

    public void delete(Long id) {
        repository.deleteById(id);
        cacheService.refreshCache();
        log.info("[AppFeatureService] Deleted feature id={} - Redis cache refreshed", id);
    }

    public AppFeature toggleActive(Long id) {
        AppFeature feature = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found: " + id));
        feature.setActive(!feature.isActive());
        AppFeature saved = repository.save(feature);
        cacheService.refreshCache();
        log.info("[AppFeatureService] Toggled feature '{}' active={} - Redis cache refreshed",
                saved.getName(), saved.isActive());
        return saved;
    }

    // ── Scheduled fallback: catches direct DB edits (every 60 seconds) ──────

    @Scheduled(fixedDelay = 60_000)
    public void scheduledCacheSync() {
        log.debug("[AppFeatureService] Scheduled cache sync running...");
        cacheService.refreshCache();
    }
}