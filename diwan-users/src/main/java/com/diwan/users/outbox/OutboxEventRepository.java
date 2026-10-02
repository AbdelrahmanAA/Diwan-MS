package com.diwan.users.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Oldest unpublished events; rows locked by another instance are skipped (SKIP LOCKED). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.id")
    List<OutboxEvent> lockPending(Pageable page);

    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt is not null and e.publishedAt < :before")
    int deletePublishedBefore(LocalDateTime before);
}
