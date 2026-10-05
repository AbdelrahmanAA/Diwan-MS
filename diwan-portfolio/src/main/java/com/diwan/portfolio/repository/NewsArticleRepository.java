package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByInstrumentIdAndUrlHash(Long instrumentId, String urlHash);

    @EntityGraph(attributePaths = "instrument")
    Page<NewsArticle> findByInstrumentIdInOrderByPublishedAtDesc(Collection<Long> instrumentIds, Pageable pageable);

    @Modifying
    @Query("delete from NewsArticle n where n.publishedAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
