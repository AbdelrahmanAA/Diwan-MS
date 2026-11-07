package com.diwan.medical.service;

import com.diwan.medical.repository.MedicalAuditRepository;
import com.diwan.medical.repository.MedicalProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Erases everything kept for an account. Safe to repeat: Kafka delivers at least once. */
@Service
public class UserDataService {

    private static final Logger log = LoggerFactory.getLogger(UserDataService.class);

    private final RecordCategories categories;
    private final MedicalProfileRepository profiles;
    private final MedicalAuditRepository audit;

    public UserDataService(RecordCategories categories, MedicalProfileRepository profiles, MedicalAuditRepository audit) {
        this.categories = categories;
        this.profiles = profiles;
        this.audit = audit;
    }

    @Transactional
    public void deleteAll(Long userId) {
        int records = categories.all().stream().mapToInt(c -> c.repository().deleteAllOfUser(userId)).sum();
        int people = profiles.deleteAllOfUser(userId);
        audit.deleteAllOfUser(userId);
        log.info("[Medical] erased user {}: {} records, {} profiles", userId, records, people);
    }
}
