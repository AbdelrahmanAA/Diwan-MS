package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FxRateRepository extends JpaRepository<FxRate, FxRate.Key> {

    /** The newest rate of every currency. */
    @Query("select r from FxRate r where r.rateDate = (select max(r2.rateDate) from FxRate r2 where r2.currency = r.currency)")
    List<FxRate> findLatestRates();
}
