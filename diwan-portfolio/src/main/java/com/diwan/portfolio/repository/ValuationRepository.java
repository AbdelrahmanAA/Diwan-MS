package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.Valuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ValuationRepository extends JpaRepository<Valuation, Long> {

    List<Valuation> findByHoldingIdOrderByValuationDateDescIdDesc(Long holdingId);

    /** The newest valuation of each of the given holdings. */
    @Query("""
            select v from Valuation v where v.holding.id in :holdingIds and v.id = (
              select max(v2.id) from Valuation v2 where v2.holding.id = v.holding.id and v2.valuationDate = (
                select max(v3.valuationDate) from Valuation v3 where v3.holding.id = v.holding.id))""")
    List<Valuation> findLatestFor(@Param("holdingIds") Collection<Long> holdingIds);

    @Query("select v from Valuation v join fetch v.holding h where v.id = :id and h.portfolio.userId = :userId")
    Optional<Valuation> findOwned(@Param("id") Long id, @Param("userId") Long userId);
}
