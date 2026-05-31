package com.diwan.users.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class GatewayValidationFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/users/login",
        "/api/users/register"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        String validated = request.getHeader("X-Gateway-Validated");
        String userIdHeader = request.getHeader("X-User-Id");

        if (!"true".equals(validated) || userIdHeader == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized: request did not pass Gateway validation");
            return;
        }

        try {
            Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id header");
            return;
        }

        chain.doFilter(req, res);
    }
}
