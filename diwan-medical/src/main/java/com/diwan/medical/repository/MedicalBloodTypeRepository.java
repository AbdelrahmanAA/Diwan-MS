package com.diwan.medical.repository;
import com.diwan.medical.entity.MedicalBloodType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalBloodTypeRepository extends JpaRepository<MedicalBloodType, Long> {
    List<MedicalBloodType> findByUserId(Long userId);
}