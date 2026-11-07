package com.diwan.medical.repository;

import com.diwan.medical.entity.MedicalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface MedicalEntryRepository<T extends MedicalEntry> extends JpaRepository<T, Long> {

    List<T> findByProfileIdOrderByIdDesc(Long profileId);

    Optional<T> findFirstByProfileIdOrderByIdDesc(Long profileId);

    /** Scoped to the owner: a foreign id looks the same as a missing one. */
    Optional<T> findByIdAndUserId(Long id, Long userId);

    long countByProfileId(Long profileId);

    @Modifying
    @Query("delete from #{#entityName} e where e.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
