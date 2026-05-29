package com.voicetransactions.repository;
import com.voicetransactions.entity.MedicalSurgery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalSurgeryRepository extends JpaRepository<MedicalSurgery, Long> {
    List<MedicalSurgery> findByUserId(Long userId);
}