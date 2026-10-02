package com.diwan.gateway.config;

import com.diwan.gateway.entity.AppFeature;
import com.diwan.gateway.repository.AppFeatureRepository;
import com.diwan.gateway.service.FeaturesCacheService;
import com.diwan.gateway.service.RouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RouteDataInitializer implements CommandLineRunner {

    private final RouteService routeService;
    private final AppFeatureRepository featureRepository;
    private final FeaturesCacheService cacheService;
    private final String usersServiceUrl;
    private final String transactionsServiceUrl;
    private final String medicalServiceUrl;
    private final String smartHomeServiceUrl;
    private final String loggingServiceUrl;

    public RouteDataInitializer(RouteService routeService,
                                AppFeatureRepository featureRepository,
                                FeaturesCacheService cacheService,
                                @Value("${USERS_SERVICE_URL:http://localhost:8083}") String usersServiceUrl,
                                @Value("${TRANSACTIONS_SERVICE_URL:http://localhost:8085}") String transactionsServiceUrl,
                                @Value("${MEDICAL_SERVICE_URL:http://localhost:8082}") String medicalServiceUrl,
                                @Value("${SMARTHOME_SERVICE_URL:http://localhost:8086}") String smartHomeServiceUrl,
                                @Value("${LOGGING_SERVICE_URL:http://localhost:8084}") String loggingServiceUrl) {
        this.routeService = routeService;
        this.featureRepository = featureRepository;
        this.cacheService = cacheService;
        this.usersServiceUrl = usersServiceUrl;
        this.transactionsServiceUrl = transactionsServiceUrl;
        this.medicalServiceUrl = medicalServiceUrl;
        this.smartHomeServiceUrl = smartHomeServiceUrl;
        this.loggingServiceUrl = loggingServiceUrl;
    }

    @Override
    public void run(String... args) {

        // ── 1. Seed service_routes (routing only) ──────────────────────────
        routeService.deactivate("auth");
        routeService.seedOrUpdate("users",        usersServiceUrl,        "/api/users",        "Users and Auth service");
        routeService.seedOrUpdate("transactions", transactionsServiceUrl, "/api/transactions", "Financial transactions service");
        routeService.seedOrUpdate("medical",      medicalServiceUrl,      "/api/medical",      "Medical records service");
        routeService.seedOrUpdate("smarthome",    smartHomeServiceUrl,    "/api/smart-home",   "Smart Home devices service");
        routeService.seedOrUpdate("logging",      loggingServiceUrl,      "/api/logs",         "Centralized request logging service");

        // ── 2. Seed app_features (dashboard cards) ─────────────────────────
        seedFeatureUpsert("smarthome",
            "Smart Home",
            "\uD83C\uDFE0", "#0EA5E9", "SmartHome", "/api/smart-home",
            "Control Smart Home devices through voice", 1);

        seedFeatureUpsert("medical",
            "Medical History",
            "\uD83C\uDFE5", "#EF4444", "MedicalHistory", "/api/medical",
            "Medical history and records", 2);

        seedFeatureUpsert("transactions",
            "Financial History",
            "\uD83D\uDCB0", "#16A34A", "Transactions", "/api/transactions",
            "Financial transactions and insights", 3);

        seedFeatureUpsert("users",
            "Update User Info",
            "\uD83D\uDC64", "#2563EB", "UpdateUserInfo", "/api/users",
            "Manage and update user profile", 4);

        // ── 3. Warm Redis cache ─────────────────────────────────────────────
        cacheService.refreshCache();
        System.out.println("[Gateway] app_features seeded and Redis cache warmed up");
    }

    private void seedFeatureUpsert(String name, String displayName, String icon,
                                   String color, String screenName, String path,
                                   String description, int sortOrder) {
        AppFeature feature = featureRepository.findByName(name).orElseGet(AppFeature::new);
        feature.setName(name);
        feature.setDisplayName(displayName);
        feature.setIcon(icon);
        feature.setColor(color);
        feature.setScreenName(screenName);
        feature.setPath(path);
        feature.setDescription(description);
        feature.setSortOrder(sortOrder);
        feature.setActive(true);
        featureRepository.save(feature);
        System.out.println("[Gateway] Upserted feature: " + name);
    }
}