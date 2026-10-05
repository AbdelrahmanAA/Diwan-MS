package com.diwan.portfolio.repository;

import com.diwan.portfolio.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByIdAsc(Long userId);

    List<Tag> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("delete from Tag t where t.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
