package com.voicetransactions.controller;

import com.voicetransactions.dto.MedicalRecordRequest;
import com.voicetransactions.dto.MedicalRecordResponse;
import com.voicetransactions.service.MedicalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/medical")
@CrossOrigin(origins = "*")
public class MedicalRecordController {

    private final MedicalRecordService service;
    public MedicalRecordController(MedicalRecordService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(@RequestBody MedicalRecordRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecordResponse>> getAll(
            @RequestParam Long userId,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(service.getByUserAndCategory(userId, category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> update(
            @PathVariable Long id,
            @RequestParam String category,
            @RequestBody MedicalRecordRequest req) {
        return ResponseEntity.ok(service.update(id, category, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam String category) {
        service.delete(id, category);
        return ResponseEntity.noContent().build();
    }
}