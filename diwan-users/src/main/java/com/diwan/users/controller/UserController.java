package com.diwan.users.controller;

import com.diwan.users.dto.*;
import com.diwan.users.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping("/api/users/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", service.register(req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", service.login(req)));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/users/logout")
    public ResponseEntity<?> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            service.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/internal/users/{userId}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable Long userId) {
        return ResponseEntity.ok(service.existsAndActive(userId));
    }

    @DeleteMapping("/api/users/{userId}")
    public ResponseEntity<?> delete(@PathVariable Long userId) {
        service.deleteUser(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "User deleted"));
    }

    @PatchMapping("/api/users/{userId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long userId) {
        service.deactivateUser(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "User deactivated"));
    }
}