package com.diwan.logging.repository;

import com.diwan.logging.entity.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    Page<RequestLog> findByUserIdOrderByRequestTimeDesc(Long userId, Pageable pageable);
    Page<RequestLog> findByTargetServiceOrderByRequestTimeDesc(String targetService, Pageable pageable);
    Page<RequestLog> findByStatusCodeGreaterThanEqualOrderByRequestTimeDesc(Integer statusCode, Pageable pageable);
    Page<RequestLog> findByRequestTimeBetweenOrderByRequestTimeDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
