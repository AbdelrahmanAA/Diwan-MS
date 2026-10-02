package com.diwan.gateway.filter;

import org.springframework.web.server.ServerWebExchange;

/**
 * The actuator (health probes, Prometheus) listens on a separate management port that is never published or
 * routed. The gateway's web filters also run for that port, so they must recognise it: no JWT is required
 * there (Prometheus cannot log in) and scrapes are not written to the request log.
 */
final class ManagementPort {

    private ManagementPort() {}

    /** @param managementPort {@code management.server.port}; a value <= 0 means "same port as the application" */
    static boolean isManagementRequest(ServerWebExchange exchange, int managementPort) {
        if (managementPort <= 0) return false;
        var local = exchange.getRequest().getLocalAddress();
        return local != null && local.getPort() == managementPort;
    }
}
