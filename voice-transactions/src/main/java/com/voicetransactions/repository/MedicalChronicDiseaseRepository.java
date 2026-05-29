package com.voicetransactions.repository;
import com.voicetransactions.entity.MedicalChronicDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalChronicDiseaseRepository extends JpaRepository<MedicalChronicDisease, Long> {
    List<MedicalChronicDisease> findByUserId(Long userId);
}