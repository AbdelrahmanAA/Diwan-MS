package com.voicetransactions.repository;

import com.voicetransactions.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ── User-scoped queries ───────────────────────────────────────────────────
    List<Transaction> findByUserIdOrderByRecordedAtDesc(Long userId);

    List<Transaction> findByUserIdAndCategoryIgnoreCaseOrderByRecordedAtDesc(Long userId, String category);

    List<Transaction> findByUserIdAndBankIgnoreCaseOrderByRecordedAtDesc(Long userId, String bank);

    List<Transaction> findByUserIdAndTypeOrderByRecordedAtDesc(Long userId, String type);

    List<Transaction> findByUserIdAndStatusIgnoreCaseOrderByRecordedAtDesc(Long userId, String status);

    List<Transaction> findByUserIdAndPaymentMethodIgnoreCaseOrderByRecordedAtDesc(Long userId, String paymentMethod);

    List<Transaction> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(Long userId, LocalDateTime from, LocalDateTime to);

    // ── Legacy (non-scoped) ───────────────────────────────────────────────────
    List<Transaction> findByCategoryIgnoreCaseOrderByRecordedAtDesc(String category);
    List<Transaction> findByBankIgnoreCaseOrderByRecordedAtDesc(String bank);
    List<Transaction> findByTypeOrderByRecordedAtDesc(String type);
    List<Transaction> findByStatusIgnoreCaseOrderByRecordedAtDesc(String status);
    List<Transaction> findByPaymentMethodIgnoreCaseOrderByRecordedAtDesc(String paymentMethod);
    List<Transaction> findByRecordedAtBetweenOrderByRecordedAtDesc(LocalDateTime from, LocalDateTime to);
}

