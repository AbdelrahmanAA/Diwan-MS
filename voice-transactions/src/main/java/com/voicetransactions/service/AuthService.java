package com.voicetransactions.service;

import com.voicetransactions.dto.AuthResponse;
import com.voicetransactions.dto.LoginRequest;
import com.voicetransactions.dto.RegisterRequest;
import com.voicetransactions.entity.User;
import com.voicetransactions.repository.UserRepository;
import com.voicetransactions.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    // ── Register ──────────────────────────────────────────────────────────────
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "البريد الإلكتروني مسجل مسبقاً");
        }
        User user = new User();
        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail());
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "البريد الإلكتروني أو كلمة المرور غير صحيحة"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "البريد الإلكتروني أو كلمة المرور غير صحيحة");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail());
    }

    // ── Forgot Password — generates a reset token ─────────────────────────────
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "لا يوجد حساب بهذا البريد الإلكتروني"));
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        // In production: send email. Here we return the token directly for development.
        return token;
    }

    // ── Reset Password ────────────────────────────────────────────────────────
    public void resetPassword(String resetToken, String newPassword) {
        User user = userRepository.findByResetToken(resetToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "رمز إعادة التعيين غير صحيح أو منتهي الصلاحية"));
        if (user.getResetTokenExpiresAt() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "انتهت صلاحية رمز إعادة التعيين");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }
}
