package com.diwan.medical.repository;
import com.diwan.medical.entity.MedicalChronicDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalChronicDiseaseRepository extends JpaRepository<MedicalChronicDisease, Long> {
    List<MedicalChronicDisease> findByUserId(Long userId);
}