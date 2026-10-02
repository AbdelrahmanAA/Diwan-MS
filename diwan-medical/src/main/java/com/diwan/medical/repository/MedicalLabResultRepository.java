package com.diwan.medical.repository;
import com.diwan.medical.entity.MedicalLabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalLabResultRepository extends JpaRepository<MedicalLabResult, Long> {
    List<MedicalLabResult> findByUserId(Long userId);
}