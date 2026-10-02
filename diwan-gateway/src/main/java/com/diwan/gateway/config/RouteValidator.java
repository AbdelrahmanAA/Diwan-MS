package com.diwan.gateway.config;

import com.diwan.gateway.entity.ServiceRoute;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A single bad row in service_routes (e.g. a path prefix ending in "/**") must not be able to stop the whole
 * gateway from starting, so rows are validated both when they are saved and when routes are built.
 */
public final class RouteValidator {

    // "/api/x" or "/api/v1/x-y": no wildcards, no trailing slash, no spaces; "/" alone would swallow everything
    private static final Pattern PATH_PREFIX = Pattern.compile("^/[A-Za-z0-9._~-]+(/[A-Za-z0-9._~-]+)*$");

    private RouteValidator() {}

    /** @return the problem, or empty when the route is valid */
    public static Optional<String> validate(ServiceRoute route) {
        if (route.getServiceName() == null || route.getServiceName().isBlank()) {
            return Optional.of("serviceName is required");
        }
        String prefix = route.getPathPrefix();
        if (prefix == null || !PATH_PREFIX.matcher(prefix).matches()) {
            return Optional.of("pathPrefix '" + prefix + "' must look like /api/service (no wildcards, no trailing slash)");
        }
        try {
            URI uri = URI.create(route.getBaseUrl() == null ? "" : route.getBaseUrl());
            if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                return Optional.of("baseUrl '" + route.getBaseUrl() + "' must be an http(s) URL with a host");
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return Optional.of("baseUrl must not contain a query or fragment");
            }
        } catch (IllegalArgumentException e) {
            return Optional.of("baseUrl '" + route.getBaseUrl() + "' is not a valid URL");
        }
        if (route.getTimeoutMs() < 0) {
            return Optional.of("timeoutMs must not be negative");
        }
        return Optional.empty();
    }
}
