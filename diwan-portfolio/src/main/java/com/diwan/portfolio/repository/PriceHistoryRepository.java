package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, PriceHistory.Key> {

    List<PriceHistory> findByInstrumentIdAndPriceDateGreaterThanEqualOrderByPriceDateAsc(Long instrumentId, LocalDate from);
}
