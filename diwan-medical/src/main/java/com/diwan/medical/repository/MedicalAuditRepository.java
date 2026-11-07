package com.diwan.medical.repository;

import com.diwan.medical.entity.MedicalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalAuditRepository extends JpaRepository<MedicalAudit, Long> {

    List<MedicalAudit> findByUserIdOrderByIdAsc(Long userId);

    @Modifying
    @Query("delete from MedicalAudit a where a.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
