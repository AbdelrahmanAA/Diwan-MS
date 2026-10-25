package com.diwan.transactions.repository;

import com.diwan.transactions.entity.BankProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BankProductRepository extends JpaRepository<BankProduct, Long> {

    List<BankProduct> findByUserId(Long userId);

    Optional<BankProduct> findByUserIdAndBankAndKindAndRef(Long userId, String bank, String kind, String ref);

    @Modifying
    @Transactional
    @Query("delete from BankProduct p where p.userId = :userId")
    int deleteByUserId(Long userId);
}
