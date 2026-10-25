package com.diwan.transactions.service;

import com.diwan.transactions.dto.ImportTransactionsRequest;
import com.diwan.transactions.dto.TransactionRequest;
import com.diwan.transactions.dto.TransactionResponse;
import com.diwan.transactions.entity.Transaction;
import com.diwan.transactions.repository.BankProductRepository;
import com.diwan.transactions.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final BankProductRepository products;

    public TransactionService(TransactionRepository repository, BankProductRepository products) {
        this.repository = repository;
        this.products = products;
    }

    // â”€â”€ Save â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public TransactionResponse save(TransactionRequest req, Long userId) {
        return toResponse(repository.save(build(req, userId).build()));
    }

    private static Transaction.Builder build(TransactionRequest req, Long userId) {
        return Transaction.builder()
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
                .recordedAt(req.getRecordedAt());
    }

    // -- Import from bank SMS (mobile app) ------------------------------------
    /**
     * Stores the transactions whose SMS fingerprint the user does not have yet. A merchant the user has seen
     * before keeps the category of their latest transaction there, so recategorizing one SMS transaction
     * (e.g. a "bill" that is really a car loan) applies to the following ones.
     */
    public Map<String, Integer> importSms(Long userId, List<ImportTransactionsRequest.Item> items) {
        int imported = 0;
        int duplicates = 0;
        Set<String> seen = new HashSet<>();
        for (ImportTransactionsRequest.Item item : items) {
            String ref = item.getSourceRef().trim().toLowerCase();
            if (!seen.add(ref)) {
                duplicates++;
                continue;
            }
            Optional<Transaction> existing = repository.findByUserIdAndSourceRef(userId, ref);
            if (existing.isPresent()) {
                duplicates++;
                fillPositionFields(existing.get(), item); // imported by an app version before positions
                continue;
            }
            Transaction.Builder tx = build(item, userId).source("sms").sourceRef(ref)
                    .accountRef(item.getAccountRef()).counterAccountRef(item.getCounterAccountRef())
                    .cardRef(item.getCardRef()).cardNetwork(item.getCardNetwork())
                    .balance(item.getBalance()).balanceCurrency(item.getBalanceCurrency()).balanceOf(item.getBalanceOf());
            if (item.getMerchantName() != null && !item.getMerchantName().isBlank()) {
                repository.findFirstByUserIdAndMerchantNameIgnoreCaseOrderByRecordedAtDesc(userId, item.getMerchantName().trim())
                        .ifPresent(previous -> tx.category(previous.getCategory()));
            }
            try {
                repository.saveAndFlush(tx.build());
                imported++;
            } catch (DataIntegrityViolationException e) {
                duplicates++; // the same SMS imported concurrently
            }
        }
        return Map.of("imported", imported, "duplicates", duplicates);
    }

    private void fillPositionFields(Transaction t, ImportTransactionsRequest.Item item) {
        boolean hasNone = t.getAccountRef() == null && t.getCardRef() == null && t.getBalanceOf() == null;
        boolean itemHas = item.getAccountRef() != null || item.getCardRef() != null || item.getBalanceOf() != null;
        if (!hasNone || !itemHas) return;
        t.setAccountRef(item.getAccountRef());
        t.setCounterAccountRef(item.getCounterAccountRef());
        t.setCardRef(item.getCardRef());
        t.setCardNetwork(item.getCardNetwork());
        t.setBalance(item.getBalance());
        t.setBalanceCurrency(item.getBalanceCurrency());
        t.setBalanceOf(item.getBalanceOf());
        repository.save(t);
    }

    /** Everything of a deleted user (user.invalidated DELETED). */
    public void deleteUserData(Long userId) {
        repository.deleteByUserId(userId);
        products.deleteByUserId(userId);
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
        res.setSource(t.getSource());
        res.setAccountRef(t.getAccountRef());
        res.setCardRef(t.getCardRef());
        res.setCardNetwork(t.getCardNetwork());
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
        if (req.getType()          != null) entity.setType(req.getType().toLowerCase());
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