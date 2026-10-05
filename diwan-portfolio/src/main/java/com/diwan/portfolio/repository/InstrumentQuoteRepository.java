package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.InstrumentQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentQuoteRepository extends JpaRepository<InstrumentQuote, Long> {
}
