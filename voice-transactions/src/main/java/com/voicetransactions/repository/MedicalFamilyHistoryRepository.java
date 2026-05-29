package com.voicetransactions.repository;
import com.voicetransactions.entity.MedicalFamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalFamilyHistoryRepository extends JpaRepository<MedicalFamilyHistory, Long> {
    List<MedicalFamilyHistory> findByUserId(Long userId);
}