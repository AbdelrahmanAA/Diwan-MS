package com.diwan.users.service;

import com.diwan.users.dto.*;
import com.diwan.users.entity.User;
import com.diwan.users.kafka.UserEventProducer;
import com.diwan.users.repository.UserRepository;
import com.diwan.users.security.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UserService {

    private final UserRepository      repo;
    private final JwtUtil             jwtUtil;
    private final PasswordEncoder     encoder;
    private final UserEventProducer   producer;
    private final StringRedisTemplate redis;

    public UserService(UserRepository repo, JwtUtil jwtUtil, PasswordEncoder encoder,
                       UserEventProducer producer, StringRedisTemplate redis) {
        this.repo = repo; this.jwtUtil = jwtUtil; this.encoder = encoder;
        this.producer = producer; this.redis = redis;
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
        String token = jwtUtil.generateToken(u.getId(), u.getEmail());
        return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        User u = repo.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!u.isActive()) throw new RuntimeException("Account is deactivated");
        if (!encoder.matches(req.getPassword(), u.getPassword()))
            throw new RuntimeException("Invalid credentials");
        String token = jwtUtil.generateToken(u.getId(), u.getEmail());
        return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail());
    }

    /**
     * Adds the token's jti to the Redis blacklist.
     * TTL = remaining token lifetime, so the key auto-expires when the token would have anyway.
     */
    public void logout(String token) {
        try {
            String jti = jwtUtil.getTokenId(token);
            Duration ttl = jwtUtil.getRemainingTtl(token);
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

    public void deleteUser(Long userId) {
        repo.deleteById(userId);
        producer.publishInvalidated(userId, "DELETED");
    }

    public void deactivateUser(Long userId) {
        repo.findById(userId).ifPresent(u -> {
            u.setActive(false);
            repo.save(u);
            producer.publishInvalidated(userId, "DEACTIVATED");
        });
    }
}