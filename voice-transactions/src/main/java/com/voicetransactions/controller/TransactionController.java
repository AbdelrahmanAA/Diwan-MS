package com.voicetransactions.controller;

import com.voicetransactions.dto.TransactionRequest;
import com.voicetransactions.dto.TransactionResponse;
import com.voicetransactions.entity.User;
import com.voicetransactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    /** POST /api/transactions */
    @PostMapping
    public ResponseEntity<Map<String, Object>> save(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {

        TransactionResponse saved = service.save(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Transaction saved successfully",
                "data", saved
        ));
    }

    /** GET /api/transactions */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String bank,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @AuthenticationPrincipal User currentUser) {

        Long userId = currentUser.getId();
        List<TransactionResponse> list;

        if (category != null && !category.isBlank())          list = service.getByCategory(userId, category);
        else if (bank != null && !bank.isBlank())             list = service.getByBank(userId, bank);
        else if (type != null && !type.isBlank())             list = service.getByType(userId, type);
        else if (status != null && !status.isBlank())         list = service.getByStatus(userId, status);
        else if (paymentMethod != null && !paymentMethod.isBlank()) list = service.getByPaymentMethod(userId, paymentMethod);
        else if (dateFrom != null && dateTo != null)          list = service.getByDateRange(userId, dateFrom, dateTo);
        else                                                  list = service.getAll(userId);

        return ResponseEntity.ok(Map.of("success", true, "count", list.size(), "data", list));
    }

    /** GET /api/transactions/summary */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getSummary(currentUser.getId())));
    }

    /** PUT /api/transactions/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {

        TransactionResponse updated = service.update(id, currentUser.getId(), request);
        return ResponseEntity.ok(Map.of("success", true, "data", updated));
    }

    /** DELETE /api/transactions/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        service.delete(id, currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Deleted"));
    }
}