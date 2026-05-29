package com.voicetransactions.repository;

import com.voicetransactions.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByUserIdOrderByRecordedAtDesc(Long userId);
    List<MedicalRecord> findByUserIdAndCategoryOrderByRecordedAtDesc(Long userId, String category);
}