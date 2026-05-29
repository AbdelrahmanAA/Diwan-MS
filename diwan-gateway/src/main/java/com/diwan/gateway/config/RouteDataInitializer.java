package com.diwan.gateway.config;

import com.diwan.gateway.entity.AppFeature;
import com.diwan.gateway.repository.AppFeatureRepository;
import com.diwan.gateway.service.FeaturesCacheService;
import com.diwan.gateway.service.RouteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RouteDataInitializer implements CommandLineRunner {

    private final RouteService routeService;
    private final AppFeatureRepository featureRepository;
    private final FeaturesCacheService cacheService;

    public RouteDataInitializer(RouteService routeService,
                                AppFeatureRepository featureRepository,
                                FeaturesCacheService cacheService) {
        this.routeService = routeService;
        this.featureRepository = featureRepository;
        this.cacheService = cacheService;
    }

    @Override
    public void run(String... args) {

        // ── 1. Seed service_routes (routing only) ──────────────────────────
        routeService.deactivate("auth");
        routeService.seedIfAbsent("users",        "http://localhost:8083", "/api/users",        "Users and Auth service");
        routeService.seedIfAbsent("transactions", "http://localhost:8085", "/api/transactions", "Financial transactions service");
        routeService.seedIfAbsent("medical",      "http://localhost:8082", "/api/medical",      "Medical records service");
        routeService.seedIfAbsent("logging",      "http://localhost:8084", "/api/logs",         "Centralized request logging service");

        // ── 2. Seed app_features (dashboard cards) ─────────────────────────
        seedFeatureIfAbsent("transactions",
            "\u0627\u0644\u0645\u0639\u0627\u0645\u0644\u0627\u062a \u0627\u0644\u0645\u0627\u0644\u064a\u0629",
            "\uD83D\uDCB0", "#4CAF50", "TransactionJourney", "/api/transactions",
            "\u0625\u062f\u0627\u0631\u0629 \u0627\u0644\u0645\u0639\u0627\u0645\u0644\u0627\u062a \u0648\u0627\u0644\u062a\u062d\u0648\u064a\u0644\u0627\u062a \u0627\u0644\u0645\u0627\u0644\u064a\u0629", 1);

        seedFeatureIfAbsent("medical",
            "\u0627\u0644\u0633\u062c\u0644 \u0627\u0644\u0637\u0628\u064a",
            "\uD83C\uDFE5", "#F44336", "MedicalHistory", "/api/medical",
            "\u0633\u062c\u0644\u0627\u062a \u0627\u0644\u0623\u062f\u0648\u064a\u0629 \u0648\u0627\u0644\u062a\u0634\u062e\u064a\u0635\u0627\u062a \u0627\u0644\u0637\u0628\u064a\u0629", 2);

        seedFeatureIfAbsent("users",
            "\u0627\u0644\u062d\u0633\u0627\u0628",
            "\uD83D\uDC64", "#2196F3", "Profile", "/api/users",
            "\u0625\u062f\u0627\u0631\u0629 \u0628\u064a\u0627\u0646\u0627\u062a \u0627\u0644\u0645\u0633\u062a\u062e\u062f\u0645", 3);

        seedFeatureIfAbsent("logging",
            "\u0633\u062c\u0644 \u0627\u0644\u0637\u0644\u0628\u0627\u062a",
            "\uD83D\uDCCB", "#FF9800", "Logs", "/api/logs",
            "\u0645\u0631\u0627\u0642\u0628\u0629 \u0637\u0644\u0628\u0627\u062a \u0627\u0644\u0646\u0638\u0627\u0645", 4);

        // ── 3. Warm Redis cache ─────────────────────────────────────────────
        cacheService.refreshCache();
        System.out.println("[Gateway] app_features seeded and Redis cache warmed up");
    }

    private void seedFeatureIfAbsent(String name, String displayName, String icon,
                                     String color, String screenName, String path,
                                     String description, int sortOrder) {
        if (!featureRepository.existsByName(name)) {
            featureRepository.save(new AppFeature(
                name, displayName, icon, color, screenName, path, description, sortOrder));
            System.out.println("[Gateway] Seeded feature: " + name);
        }
    }
}