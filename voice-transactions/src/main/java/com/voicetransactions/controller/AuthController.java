package com.voicetransactions.controller;

import com.voicetransactions.dto.AuthResponse;
import com.voicetransactions.dto.LoginRequest;
import com.voicetransactions.dto.RegisterRequest;
import com.voicetransactions.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "تم إنشاء الحساب بنجاح",
                "data", auth
        ));
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "تم تسجيل الدخول بنجاح",
                "data", auth
        ));
    }

    /** POST /api/auth/forgot-password   body: { "email": "..." } */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "البريد الإلكتروني مطلوب"));
        }
        String resetToken = authService.forgotPassword(email);
        // In production DO NOT return the token — send by email
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "تم إرسال رابط إعادة تعيين كلمة المرور",
                "resetToken", resetToken   // dev-only
        ));
    }

    /** POST /api/auth/reset-password   body: { "resetToken": "...", "newPassword": "..." } */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("resetToken"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "تم تغيير كلمة المرور بنجاح"
        ));
    }
}
