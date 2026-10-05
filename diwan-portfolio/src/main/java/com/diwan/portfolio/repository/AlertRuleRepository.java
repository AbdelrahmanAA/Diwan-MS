package com.diwan.portfolio.repository;

import com.diwan.portfolio.domain.AlertType;
import com.diwan.portfolio.entity.AlertRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    @EntityGraph(attributePaths = {"instrument", "holding", "portfolio"})
    List<AlertRule> findByUserIdOrderByIdAsc(Long userId);

    @EntityGraph(attributePaths = {"instrument", "holding", "portfolio"})
    Optional<AlertRule> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /** Active rules affected by a new quote of the instrument: symbol rules and rules on holdings of it. */
    @EntityGraph(attributePaths = {"instrument", "holding", "holding.instrument", "portfolio"})
    @Query("""
            select r from AlertRule r left join r.holding h
            where r.active = true and (r.instrument.id = :instrumentId or h.instrument.id = :instrumentId)""")
    List<AlertRule> findActiveForInstrument(@Param("instrumentId") Long instrumentId);

    @EntityGraph(attributePaths = {"portfolio"})
    List<AlertRule> findByActiveTrueAndType(AlertType type);

    @Modifying
    @Query("delete from AlertRule r where r.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);

    @Modifying
    @Query("update AlertRule r set r.active = false where r.userId = :userId")
    int deactivateAllOfUser(@Param("userId") Long userId);
}
