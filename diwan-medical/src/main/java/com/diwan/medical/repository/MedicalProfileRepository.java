package com.diwan.medical.repository;

import com.diwan.medical.domain.ProfileRelation;
import com.diwan.medical.entity.MedicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalProfileRepository extends JpaRepository<MedicalProfile, Long> {

    List<MedicalProfile> findByUserIdOrderByIdAsc(Long userId);

    Optional<MedicalProfile> findByIdAndUserId(Long id, Long userId);

    Optional<MedicalProfile> findByUserIdAndRelation(Long userId, ProfileRelation relation);

    long countByUserId(Long userId);

    /** The unique key on a user's SELF row makes concurrent first requests safe. */
    @Modifying
    @Query(value = """
            insert ignore into medical_profile (user_id, relation, created_at, updated_at)
            values (:userId, 'SELF', utc_timestamp(6), utc_timestamp(6))""", nativeQuery = true)
    void createSelf(@Param("userId") Long userId);

    @Modifying
    @Query("delete from MedicalProfile p where p.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
