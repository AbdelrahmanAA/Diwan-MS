package com.diwan.gateway.repository;

import com.diwan.gateway.entity.AppFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppFeatureRepository extends JpaRepository<AppFeature, Long> {
    List<AppFeature> findByActiveTrueOrderBySortOrderAsc();
    Optional<AppFeature> findByName(String name);
    boolean existsByName(String name);
}