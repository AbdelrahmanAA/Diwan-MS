package com.diwan.medical.service;

import com.diwan.medical.entity.MedicalAudit;
import com.diwan.medical.repository.MedicalAuditRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Records changes inside the caller's transaction, so a change and its audit row succeed or fail together. */
@Service
public class AuditService {

    private final MedicalAuditRepository audit;
    private final Clock clock = Clock.systemUTC();

    public AuditService(MedicalAuditRepository audit) {
        this.audit = audit;
    }

    public void record(Long userId, Long profileId, String action, String category, Long recordId) {
        audit.save(new MedicalAudit(userId, profileId, action, category, recordId, MDC.get("requestId"), clock.instant()));
    }
}
