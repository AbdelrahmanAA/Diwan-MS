package com.diwan.gateway.service;

import com.diwan.gateway.entity.AppFeature;
import com.diwan.gateway.repository.AppFeatureRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeaturesCacheService {

    private static final Logger log = LoggerFactory.getLogger(FeaturesCacheService.class);
    static final String CACHE_KEY = "diwan:features";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final AppFeatureRepository featureRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeaturesCacheService(StringRedisTemplate redis, AppFeatureRepository featureRepository) {
        this.redis = redis;
        this.featureRepository = featureRepository;
    }

    public List<Map<String, Object>> getFeatures() {
        try {
            String cached = redis.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                log.debug("[Features] Cache HIT");
                return objectMapper.readValue(cached, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("[Features] Redis read failed: {}", e.getMessage());
        }
        log.debug("[Features] Cache MISS - loading from DB");
        List<Map<String, Object>> features = loadFromDb();
        writeToCache(features);
        return features;
    }

    public void evictCache() {
        try {
            redis.delete(CACHE_KEY);
            log.info("[Features] Cache evicted");
        } catch (Exception e) {
            log.warn("[Features] Redis evict failed: {}", e.getMessage());
        }
    }

    public void refreshCache() {
        evictCache();
        List<Map<String, Object>> features = loadFromDb();
        writeToCache(features);
        log.info("[Features] Cache refreshed - {} active features", features.size());
    }

    private List<Map<String, Object>> loadFromDb() {
        return featureRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toFeatureMap).collect(Collectors.toList());
    }

    private Map<String, Object> toFeatureMap(AppFeature feature) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",          feature.getName());
        map.put("displayName", feature.getDisplayName());
        map.put("icon",        feature.getIcon());
        map.put("color",       feature.getColor());
        map.put("description", feature.getDescription());
        map.put("screen",      feature.getScreenName());
        map.put("path",        feature.getPath());
        map.put("active",      feature.isActive());
        return map;
    }

    private void writeToCache(List<Map<String, Object>> features) {
        try {
            String json = objectMapper.writeValueAsString(features);
            redis.opsForValue().set(CACHE_KEY, json, TTL);
            log.debug("[Features] Written to Redis ({} features)", features.size());
        } catch (Exception e) {
            log.warn("[Features] Redis write failed: {}", e.getMessage());
        }
    }
}