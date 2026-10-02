package com.diwan.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rejects requests that were not signed by the gateway (see {@link GatewaySigner}).
 * On public paths the request passes through, but the trusted identity headers are hidden
 * from the application so they cannot be forged there either.
 */
public class GatewayTrustFilter implements Filter {

    private static final long MAX_SKEW_MILLIS = 120_000;
    private static final Set<String> TRUSTED_LOWER = GatewaySigner.TRUSTED_HEADERS.stream()
            .map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

    private final GatewaySigner signer;
    private final List<String> publicPaths;
    private final String requiredRole;

    /** @param publicPaths exact paths or path prefixes (matched on a "/" boundary) that skip verification */
    public GatewayTrustFilter(GatewaySigner signer, Collection<String> publicPaths, String requiredRole) {
        this.signer = signer;
        this.publicPaths = List.copyOf(publicPaths);
        this.requiredRole = requiredRole;
    }

    public GatewayTrustFilter(GatewaySigner signer, Collection<String> publicPaths) {
        this(signer, publicPaths, null);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (isPublic(request.getRequestURI())) {
            chain.doFilter(new StrippedRequest(request), res);
            return;
        }

        String userId = request.getHeader(GatewaySigner.HDR_USER_ID);
        String email = request.getHeader(GatewaySigner.HDR_EMAIL);
        String role = request.getHeader(GatewaySigner.HDR_ROLE);
        String tsHeader = request.getHeader(GatewaySigner.HDR_TIMESTAMP);
        String signature = request.getHeader(GatewaySigner.HDR_SIGNATURE);

        if (userId == null || tsHeader == null || signature == null) {
            response.sendError(401, "Unauthorized: request did not pass Gateway validation");
            return;
        }
        long ts;
        try {
            ts = Long.parseLong(tsHeader);
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid gateway headers");
            return;
        }
        if (Math.abs(System.currentTimeMillis() - ts) > MAX_SKEW_MILLIS
                || !signer.verify(userId, email, role, ts, signature)) {
            response.sendError(401, "Unauthorized: invalid gateway signature");
            return;
        }
        if (requiredRole != null && !requiredRole.equals(role)) {
            response.sendError(403, "Forbidden");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean isPublic(String path) {
        for (String p : publicPaths) {
            if (path.equals(p) || path.startsWith(p.endsWith("/") ? p : p + "/")) return true;
        }
        return false;
    }

    private static final class StrippedRequest extends HttpServletRequestWrapper {
        StrippedRequest(HttpServletRequest request) {
            super(request);
        }

        private static boolean hidden(String name) {
            return name != null && TRUSTED_LOWER.contains(name.toLowerCase(Locale.ROOT));
        }

        @Override
        public String getHeader(String name) {
            return hidden(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return hidden(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
                    .filter(n -> !hidden(n)).collect(Collectors.toList()));
        }
    }
}
