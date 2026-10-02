package com.diwan.smarthome.security;

import com.diwan.common.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtTokenService {

    private final JwtService jwtService;

    public JwtTokenService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Long extractUserIdFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        try {
            Long userId = jwtService.extractUserId(token);
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token does not include userId");
            }
            return userId;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }
}
