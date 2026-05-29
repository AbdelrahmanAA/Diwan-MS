package com.voicetransactions.repository;
import com.voicetransactions.entity.MedicalBloodType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalBloodTypeRepository extends JpaRepository<MedicalBloodType, Long> {
    List<MedicalBloodType> findByUserId(Long userId);
}