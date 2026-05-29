package com.diwan.smarthome.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Trusts the Gateway's JWT validation.
 * Requires X-Gateway-Validated = "true" and X-User-Id present.
 */
@Component
public class UserValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Allow health check without auth
        if (request.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(req, res);
            return;
        }

        String validated   = request.getHeader("X-Gateway-Validated");
        String userIdHeader = request.getHeader("X-User-Id");

        if (!"true".equals(validated) || userIdHeader == null) {
            response.sendError(401, "Unauthorized: missing gateway validation");
            return;
        }

        try {
            Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid X-User-Id header");
            return;
        }

        chain.doFilter(req, res);
    }
}
