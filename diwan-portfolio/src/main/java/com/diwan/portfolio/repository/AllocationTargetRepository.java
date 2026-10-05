package com.diwan.portfolio.repository;

import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.entity.AllocationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AllocationTargetRepository extends JpaRepository<AllocationTarget, Long> {

    List<AllocationTarget> findByPortfolioId(Long portfolioId);

    @Modifying(flushAutomatically = true)
    @Query("delete from AllocationTarget t where t.portfolio.id = :portfolioId and t.dimension = :dimension")
    void deleteDimension(@Param("portfolioId") Long portfolioId, @Param("dimension") AllocationDimension dimension);
}
