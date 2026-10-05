package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByPortfolioIdOrderByIdAsc(Long portfolioId);

    @Query("select g from Goal g join fetch g.portfolio p where g.id = :id and p.userId = :userId")
    Optional<Goal> findOwned(@Param("id") Long id, @Param("userId") Long userId);
}
