package com.voicetransactions.service;

import com.voicetransactions.dto.TransactionRequest;
import com.voicetransactions.dto.TransactionResponse;
import com.voicetransactions.entity.Transaction;
import com.voicetransactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    // â”€â”€ Save â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public TransactionResponse save(TransactionRequest req, Long userId) {
        Transaction entity = Transaction.builder()
                .userId(userId)
                .bank(req.getBank() != null ? req.getBank() : "Unknown")
                .type(req.getType())
                .category(req.getCategory().toLowerCase())
                .amount(req.getAmount())
                .currency(req.getCurrency().toUpperCase())
                .merchantName(req.getMerchantName())
                .paymentMethod(req.getPaymentMethod())
                .status(req.getStatus())
                .description(req.getDescription())
                .notes(req.getNotes())
                .referenceNumber(req.getReferenceNumber())
                .tags(req.getTags())
                .recordedAt(req.getRecordedAt())
                .build();

        return toResponse(repository.save(entity));
    }

    // â”€â”€ Queries (user-scoped) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public List<TransactionResponse> getByCategory(Long userId, String category) {
        return repository.findByUserIdAndCategoryIgnoreCaseOrderByRecordedAtDesc(userId, category)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByBank(Long userId, String bank) {
        return repository.findByUserIdAndBankIgnoreCaseOrderByRecordedAtDesc(userId, bank)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByType(Long userId, String type) {
        return repository.findByUserIdAndTypeOrderByRecordedAtDesc(userId, type)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByStatus(Long userId, String status) {
        return repository.findByUserIdAndStatusIgnoreCaseOrderByRecordedAtDesc(userId, status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByPaymentMethod(Long userId, String paymentMethod) {
        return repository.findByUserIdAndPaymentMethodIgnoreCaseOrderByRecordedAtDesc(userId, paymentMethod)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        return repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(userId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getAll(Long userId) {
        return repository.findByUserIdOrderByRecordedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // â”€â”€ Summary (user-scoped) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public Map<String, Map<String, BigDecimal>> getSummary(Long userId) {
        Map<String, Map<String, BigDecimal>> summary = new HashMap<>();
        for (Transaction t : repository.findByUserIdOrderByRecordedAtDesc(userId)) {
            summary
                .computeIfAbsent(t.getCurrency(), k -> new HashMap<>())
                .merge(t.getType(), t.getAmount(), BigDecimal::add);
        }
        return summary;
    }

    // â”€â”€ Mapper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private TransactionResponse toResponse(Transaction t) {
        TransactionResponse res = new TransactionResponse();
        res.setId(t.getId());
        res.setBank(t.getBank());
        res.setType(t.getType());
        res.setCategory(t.getCategory());
        res.setAmount(t.getAmount());
        res.setCurrency(t.getCurrency());
        res.setMerchantName(t.getMerchantName());
        res.setPaymentMethod(t.getPaymentMethod());
        res.setStatus(t.getStatus());
        res.setDescription(t.getDescription());
        res.setNotes(t.getNotes());
        res.setReferenceNumber(t.getReferenceNumber());
        res.setTags(t.getTags());
        res.setRecordedAt(t.getRecordedAt());
        res.setCreatedAt(t.getCreatedAt());
        res.setUpdatedAt(t.getUpdatedAt());
        return res;
    }

    // -- Update (user-scoped) -------------------------------------------------
    public TransactionResponse update(Long id, Long userId, TransactionRequest req) {
        Transaction entity = repository.findById(id)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Transaction not found or unauthorized"));
        if (req.getAmount()        != null) entity.setAmount(req.getAmount());
        if (req.getCategory()      != null) entity.setCategory(req.getCategory().toLowerCase());
        if (req.getMerchantName()  != null) entity.setMerchantName(req.getMerchantName());
        if (req.getNotes()         != null) entity.setNotes(req.getNotes());
        if (req.getStatus()        != null) entity.setStatus(req.getStatus());
        if (req.getPaymentMethod() != null) entity.setPaymentMethod(req.getPaymentMethod());
        if (req.getBank()          != null) entity.setBank(req.getBank());
        return toResponse(repository.save(entity));
    }

    // -- Delete (user-scoped) -------------------------------------------------
    public void delete(Long id, Long userId) {
        Transaction entity = repository.findById(id)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Transaction not found or unauthorized"));
        repository.delete(entity);
    }
}