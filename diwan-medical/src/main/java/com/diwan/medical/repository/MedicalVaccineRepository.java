package com.diwan.medical.repository;
import com.diwan.medical.entity.MedicalVaccine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalVaccineRepository extends JpaRepository<MedicalVaccine, Long> {
    List<MedicalVaccine> findByUserId(Long userId);
}