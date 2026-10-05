package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.Holding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    @EntityGraph(attributePaths = {"instrument", "tags"})
    List<Holding> findByPortfolioIdOrderByIdAsc(Long portfolioId);

    long countByPortfolioId(Long portfolioId);

    @EntityGraph(attributePaths = {"instrument", "tags", "portfolio"})
    @Query("select h from Holding h where h.id = :id and h.portfolio.userId = :userId")
    Optional<Holding> findOwned(@Param("id") Long id, @Param("userId") Long userId);

    /** Serializes concurrent transaction changes on the same holding while its position is recomputed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holding h where h.id = :id and h.portfolio.userId = :userId")
    Optional<Holding> lockOwned(@Param("id") Long id, @Param("userId") Long userId);

    /** All open holdings of a user (all portfolios) - used for news and alert scopes. */
    @EntityGraph(attributePaths = {"instrument", "portfolio"})
    @Query("select h from Holding h where h.portfolio.userId = :userId and h.closed = false")
    List<Holding> findOpenOfUser(@Param("userId") Long userId);
}
