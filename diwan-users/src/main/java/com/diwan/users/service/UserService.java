package com.diwan.users.service;

import com.diwan.users.dto.*;
import com.diwan.users.entity.User;
import com.diwan.users.outbox.OutboxService;
import org.springframework.transaction.annotation.Transactional;
import com.diwan.users.repository.UserRepository;
import com.diwan.common.security.JwtService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UserService {

    private final UserRepository      repo;
    private final JwtService          jwtService;
    private final PasswordEncoder     encoder;
    private final OutboxService       outbox;
    private final StringRedisTemplate redis;

    public UserService(UserRepository repo, JwtService jwtService, PasswordEncoder encoder,
                       OutboxService outbox, StringRedisTemplate redis) {
        this.repo = repo; this.jwtService = jwtService; this.encoder = encoder;
        this.outbox = outbox; this.redis = redis;
    }

    // ── Auth ────────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest req) {
        if (repo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already exists");
        User u = new User();
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setPassword(encoder.encode(req.getPassword()));
        repo.save(u);
        String token = jwtService.generateToken(u.getId(), u.getEmail());
        return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        User u = repo.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!u.isActive()) throw new RuntimeException("Account is deactivated");
        if (!encoder.matches(req.getPassword(), u.getPassword()))
            throw new RuntimeException("Invalid credentials");
        String token = jwtService.generateToken(u.getId(), u.getEmail());
        return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail());
    }

    /**
     * Adds the token's jti to the Redis blacklist.
     * TTL = remaining token lifetime, so the key auto-expires when the token would have anyway.
     */
    public void logout(String token) {
        try {
            String jti = jwtService.getTokenId(token);
            Duration ttl = jwtService.getRemainingTtl(token);
            if (ttl.isZero() || ttl.isNegative()) return; // already expired - no need
            redis.opsForValue().set("blacklist:" + jti, "1", ttl);
        } catch (Exception e) {
            // invalid token - nothing to blacklist
        }
    }

    // ── User management ─────────────────────────────────────────────────────

    public boolean existsAndActive(Long userId) {
        return repo.findById(userId).map(User::isActive).orElse(false);
    }

    @Transactional
    public void deleteUser(Long userId) {
        repo.deleteById(userId);
        outbox.userInvalidated(userId, "DELETED");
    }

    @Transactional
    public void deactivateUser(Long userId) {
        repo.findById(userId).ifPresent(u -> {
            u.setActive(false);
            repo.save(u);
            outbox.userInvalidated(userId, "DEACTIVATED");
        });
    }
}