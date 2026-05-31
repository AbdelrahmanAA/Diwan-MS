package com.diwan.smarthome.repository;

import com.diwan.smarthome.entity.SmartDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SmartDeviceRepository extends JpaRepository<SmartDevice, Long> {

    List<SmartDevice> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    Optional<SmartDevice> findByIdAndUserId(Long id, Long userId);

    Optional<SmartDevice> findByDeviceIdAndUserId(String deviceId, Long userId);

    Optional<SmartDevice> findByDeviceIdAndUserIdAndActiveTrue(String deviceId, Long userId);
}
