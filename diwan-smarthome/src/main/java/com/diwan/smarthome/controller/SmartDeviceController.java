package com.diwan.smarthome.controller;

import com.diwan.smarthome.dto.DeviceResponse;
import com.diwan.smarthome.dto.DeviceStateRequest;
import com.diwan.smarthome.dto.RegisterDeviceRequest;
import com.diwan.smarthome.service.SmartDeviceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smart-home/devices")
public class SmartDeviceController {

    private final SmartDeviceService service;

    public SmartDeviceController(SmartDeviceService service) {
        this.service = service;
    }

    // ── GET /api/smart-home/devices  →  list user's devices ──────────────────
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> list(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(service.listDevices(userId));
    }

    // ── POST /api/smart-home/devices  →  register device ─────────────────────
    @PostMapping
    public ResponseEntity<DeviceResponse> register(
            HttpServletRequest request,
            @RequestBody RegisterDeviceRequest body) {
        Long userId = extractUserId(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(userId, body));
    }

    // ── DELETE /api/smart-home/devices/{id}  →  remove device ────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(HttpServletRequest request, @PathVariable Long id) {
        Long userId = extractUserId(request);
        service.removeDevice(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /api/smart-home/devices/{id}/name  →  rename device ─────────────
    @PatchMapping("/{id}/name")
    public ResponseEntity<DeviceResponse> rename(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(service.renameDevice(userId, id, body.get("name")));
    }

    // ── PATCH /api/smart-home/devices/{id}/state  →  update state from MQTT ──
    @PatchMapping("/{id}/state")
    public ResponseEntity<DeviceResponse> updateState(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody DeviceStateRequest body) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(service.updateState(userId, id, body.getState()));
    }

    // ── Helper: extract user ID set by Gateway ────────────────────────────────
    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null) throw new IllegalStateException("Missing X-User-Id header");
        return Long.parseLong(header);
    }
}
