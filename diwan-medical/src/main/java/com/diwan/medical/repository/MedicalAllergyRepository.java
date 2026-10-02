package com.diwan.medical.repository;
import com.diwan.medical.entity.MedicalAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalAllergyRepository extends JpaRepository<MedicalAllergy, Long> {
    List<MedicalAllergy> findByUserId(Long userId);
}