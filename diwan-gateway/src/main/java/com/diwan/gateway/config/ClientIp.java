package com.diwan.gateway.config;

import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

/**
 * Real client IP. X-Forwarded-For is client-controlled, so only the entries appended by our own reverse
 * proxies are trusted (the last {@code trustedProxies} ones); anything the client put in front is ignored.
 */
public final class ClientIp {

    private final XForwardedRemoteAddressResolver resolver;

    public ClientIp(int trustedProxies) {
        this.resolver = XForwardedRemoteAddressResolver.maxTrustedIndex(Math.max(1, trustedProxies));
    }

    public String of(ServerWebExchange exchange) {
        InetSocketAddress address = resolver.resolve(exchange);
        return address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : "unknown";
    }
}
