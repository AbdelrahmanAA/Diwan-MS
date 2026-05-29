package com.voicetransactions.repository;
import com.voicetransactions.entity.MedicalMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalMedicationRepository extends JpaRepository<MedicalMedication, Long> {
    List<MedicalMedication> findByUserId(Long userId);
}