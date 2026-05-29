package com.diwan.logging.controller;

import com.diwan.logging.entity.RequestLog;
import com.diwan.logging.repository.RequestLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class RequestLogController {

    private final RequestLogRepository repository;

    public RequestLogController(RequestLogRepository repository) {
        this.repository = repository;
    }

    /**
     * GET /api/logs?page=0&size=20
     * Returns all logs paginated
     */
    @GetMapping
    public ResponseEntity<?> getAllLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RequestLog> result = repository.findAll(
                PageRequest.of(page, size, Sort.by("requestTime").descending()));
        return ResponseEntity.ok(buildPageResponse(result));
    }

    /**
     * GET /api/logs/user/{userId}
     * Returns logs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RequestLog> result = repository.findByUserIdOrderByRequestTimeDesc(
                userId, PageRequest.of(page, size));
        return ResponseEntity.ok(buildPageResponse(result));
    }

    /**
     * GET /api/logs/service/{serviceName}
     * Returns logs for a specific target service
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<?> getByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RequestLog> result = repository.findByTargetServiceOrderByRequestTimeDesc(
                serviceName, PageRequest.of(page, size));
        return ResponseEntity.ok(buildPageResponse(result));
    }

    /**
     * GET /api/logs/errors
     * Returns only error logs (status >= 400)
     */
    @GetMapping("/errors")
    public ResponseEntity<?> getErrors(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RequestLog> result = repository.findByStatusCodeGreaterThanEqualOrderByRequestTimeDesc(
                400, PageRequest.of(page, size));
        return ResponseEntity.ok(buildPageResponse(result));
    }

    /**
     * GET /api/logs/range?from=2025-01-01T00:00:00&to=2025-12-31T23:59:59
     */
    @GetMapping("/range")
    public ResponseEntity<?> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RequestLog> result = repository.findByRequestTimeBetweenOrderByRequestTimeDesc(
                from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(buildPageResponse(result));
    }

    /**
     * GET /api/logs/stats
     * Quick stats: total, errors, avg duration
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long total  = repository.count();
        long errors = repository.findByStatusCodeGreaterThanEqualOrderByRequestTimeDesc(
                400, PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        return ResponseEntity.ok(Map.of(
                "totalRequests", total,
                "errorRequests", errors,
                "successRequests", total - errors
        ));
    }

    // Helper
    private Map<String, Object> buildPageResponse(Page<RequestLog> page) {
        return Map.of(
                "content",       page.getContent(),
                "totalElements", page.getTotalElements(),
                "totalPages",    page.getTotalPages(),
                "currentPage",   page.getNumber()
        );
    }
}
