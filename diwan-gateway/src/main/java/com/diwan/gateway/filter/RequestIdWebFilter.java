package com.diwan.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns the correlation id of a request: kept from the client when it is a sane value, otherwise generated.
 * It is forwarded to the services (their log lines carry it), returned to the client in the response, and
 * stored with the gateway's request log. Runs first so that every later filter sees it.
 */
@Component
public class RequestIdWebFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = incoming != null && VALID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        exchange.getResponse().getHeaders().set(HEADER, id);
        ServerWebExchange withId = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, id)))
                .build();
        return chain.filter(withId);
    }

    @Override
    public int getOrder() {
        return -5; // before JwtAuthFilter (-1)
    }
}
