package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.PortfolioTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

    /** Replay order: by trade date, then in the order they were entered. */
    List<PortfolioTransaction> findByHoldingIdOrderByTradeDateAscIdAsc(Long holdingId);

    @Query("select t from PortfolioTransaction t join fetch t.holding h where t.id = :id and h.portfolio.userId = :userId")
    Optional<PortfolioTransaction> findOwned(@Param("id") Long id, @Param("userId") Long userId);
}
