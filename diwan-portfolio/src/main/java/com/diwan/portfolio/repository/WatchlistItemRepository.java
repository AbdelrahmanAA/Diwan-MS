package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.WatchlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    @EntityGraph(attributePaths = "instrument")
    List<WatchlistItem> findByUserIdOrderByIdAsc(Long userId);

    Optional<WatchlistItem> findByUserIdAndInstrumentId(Long userId, Long instrumentId);

    @Modifying
    @Query("delete from WatchlistItem w where w.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
