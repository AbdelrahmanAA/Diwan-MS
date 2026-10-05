package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /** Bulk delete; holdings, transactions, targets, goals... go with it through ON DELETE CASCADE. */
    @Modifying
    @Query("delete from Portfolio p where p.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
