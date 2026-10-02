package com.diwan.common.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Correlation id: the gateway assigns {@code X-Request-Id}; every service puts it in the logging MDC
 * (key {@value #MDC_KEY}, used by {@code logging.pattern.level}) and echoes it in the response.
 * A request that reaches a service without one (or with a malformed one) gets a fresh id.
 */
public class RequestIdFilter implements Filter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String id = ((HttpServletRequest) req).getHeader(HEADER);
        if (id == null || !VALID.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        ((HttpServletResponse) res).setHeader(HEADER, id);
        MDC.put(MDC_KEY, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
