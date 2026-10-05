package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.PortfolioAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PortfolioAlertRepository extends JpaRepository<PortfolioAlert, Long> {

    Page<PortfolioAlert> findByUserIdOrderByTriggeredAtDescIdDesc(Long userId, Pageable pageable);

    Page<PortfolioAlert> findByUserIdAndReadAtIsNullOrderByTriggeredAtDescIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<PortfolioAlert> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update PortfolioAlert a set a.readAt = :now where a.userId = :userId and a.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from PortfolioAlert a where a.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from PortfolioAlert a where a.triggeredAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
