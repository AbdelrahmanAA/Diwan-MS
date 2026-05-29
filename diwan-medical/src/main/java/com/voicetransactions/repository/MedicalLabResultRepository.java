package com.voicetransactions.repository;
import com.voicetransactions.entity.MedicalLabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalLabResultRepository extends JpaRepository<MedicalLabResult, Long> {
    List<MedicalLabResult> findByUserId(Long userId);
}