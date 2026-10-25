package com.diwan.transactions.controller;
import com.diwan.transactions.dto.ImportTransactionsRequest;
import com.diwan.transactions.dto.Positions;
import com.diwan.transactions.dto.TransactionRequest;
import com.diwan.transactions.dto.TransactionResponse;
import com.diwan.transactions.service.PositionService;
import com.diwan.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    private final TransactionService service;
    private final PositionService positions;
    public TransactionController(TransactionService service, PositionService positions){
        this.service = service;
        this.positions = positions;
    }

    /** Cards, accounts and loans per bank from the user's bank SMS (see PositionService). */
    @GetMapping("/positions")
    public ResponseEntity<Map<String, Object>> positions(@RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.ok(Map.of("success", true, "data", positions.positions(userId)));
    }

    /** The account balance / card limit / nickname the user enters once. */
    @PutMapping("/positions/products")
    public ResponseEntity<Map<String, Object>> saveProduct(
            @Valid @RequestBody Positions.ProductRequest request,
            @RequestHeader("X-User-Id") Long userId){
        positions.saveProduct(userId, request);
        return ResponseEntity.ok(Map.of("success", true, "data", positions.positions(userId)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader("X-User-Id") Long userId){
        TransactionResponse saved = service.save(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success",true,"data",saved));
    }

    /** Batch from the mobile app's bank SMS detection; already imported messages are skipped. */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importSms(
            @Valid @RequestBody ImportTransactionsRequest request,
            @RequestHeader("X-User-Id") Long userId){
        Map<String, Integer> result = service.importSms(userId, request.getTransactions());
        return ResponseEntity.ok(Map.of("success", true,
                "imported", result.get("imported"), "duplicates", result.get("duplicates")));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String bank,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestHeader("X-User-Id") Long userId){
        List<TransactionResponse> list;
        if(category != null && !category.isBlank())               list = service.getByCategory(userId, category);
        else if(bank != null && !bank.isBlank())                  list = service.getByBank(userId, bank);
        else if(type != null && !type.isBlank())                  list = service.getByType(userId, type);
        else if(status != null && !status.isBlank())              list = service.getByStatus(userId, status);
        else if(paymentMethod != null && !paymentMethod.isBlank()) list = service.getByPaymentMethod(userId, paymentMethod);
        else if(dateFrom != null && dateTo != null)               list = service.getByDateRange(userId, dateFrom, dateTo);
        else                                                      list = service.getAll(userId);
        return ResponseEntity.ok(Map.of("success",true,"count",list.size(),"data",list));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.ok(Map.of("success",true,"data",service.getSummary(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody TransactionRequest request,
            @RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.ok(Map.of("success",true,"data",service.update(id, userId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId){
        service.delete(id, userId);
        return ResponseEntity.ok(Map.of("success",true,"message","Deleted"));
    }
}