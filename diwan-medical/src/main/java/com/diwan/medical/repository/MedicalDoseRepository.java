package com.diwan.medical.repository;

import com.diwan.medical.entity.MedicalDose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicalDoseRepository extends JpaRepository<MedicalDose, Long> {

    Optional<MedicalDose> findByMedicationIdAndScheduledAt(Long medicationId, LocalDateTime scheduledAt);

    List<MedicalDose> findByProfileIdAndScheduledAtBetweenOrderByScheduledAtAsc(Long profileId, LocalDateTime from,
                                                                               LocalDateTime to);

    @Modifying
    @Query("delete from MedicalDose d where d.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
