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
    @Query("""
            select a from NewsArticle a where a.instrument.id in :ids and a.id = (
              select min(b.id) from NewsArticle b where b.urlHash = a.urlHash and b.instrument.id in :ids)
            order by a.publishedAt desc""")
    Page<NewsArticle> findFeed(@Param("ids") Collection<Long> instrumentIds, Pageable pageable);

    @Modifying
    @Query("delete from NewsArticle n where n.publishedAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
