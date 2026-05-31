package com.diwan.smarthome.service;

import com.diwan.smarthome.dto.DeviceResponse;
import com.diwan.smarthome.dto.RegisterDeviceRequest;
import com.diwan.smarthome.entity.SmartDevice;
import com.diwan.smarthome.repository.SmartDeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SmartDeviceService {

    private static final String TOPIC_PREFIX = "myhome/SmartLamp_";

    private final SmartDeviceRepository repo;
    private final MqttCommandPublisher mqttCommandPublisher;

    public SmartDeviceService(SmartDeviceRepository repo, MqttCommandPublisher mqttCommandPublisher) {
        this.repo = repo;
        this.mqttCommandPublisher = mqttCommandPublisher;
    }

    /** Register a new device for this user. */
    public DeviceResponse register(Long userId, RegisterDeviceRequest req) {
        if (req.getDeviceId() == null || req.getDeviceId().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
        if (req.getName() == null || req.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");

        // Prevent duplicate registrations for same user
        if (repo.findByDeviceIdAndUserId(req.getDeviceId(), userId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Device already registered for this user");

        SmartDevice device = new SmartDevice();
        device.setUserId(userId);
        device.setDeviceId(req.getDeviceId().trim());
        device.setName(req.getName().trim());
        device.setMqttTopic(TOPIC_PREFIX + req.getDeviceId().trim());
        return DeviceResponse.from(repo.save(device));
    }

    /** List all active devices for this user. */
    public List<DeviceResponse> listDevices(Long userId) {
        return repo.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                   .stream().map(DeviceResponse::from).collect(Collectors.toList());
    }

    /** Soft-delete (deactivate) a device. */
    public void removeDevice(Long userId, Long deviceId) {
        SmartDevice d = repo.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        d.setActive(false);
        repo.save(d);
    }

    /** Rename a device. */
    public DeviceResponse renameDevice(Long userId, Long deviceId, String newName) {
        if (newName == null || newName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        SmartDevice d = repo.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        d.setName(newName.trim());
        return DeviceResponse.from(repo.save(d));
    }

    /** Update the last-known state (called when MQTT state message received). */
    public DeviceResponse updateState(Long userId, Long deviceId, String state) {
        if (state == null || state.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state is required");
        }
        String normalized = state.toUpperCase();
        if (!normalized.equals("ON") && !normalized.equals("OFF") && !normalized.equals("UNKNOWN"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state must be ON, OFF or UNKNOWN");

        SmartDevice d = repo.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        d.setState(normalized);
        return DeviceResponse.from(repo.save(d));
    }

    /** Resolve active device by external ID, persist state and publish command to MQTT. */
    public DeviceResponse controlByExternalDeviceId(Long userId, String externalDeviceId, String state) {
        if (externalDeviceId == null || externalDeviceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endpointId/deviceId is required");
        }
        if (state == null || state.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state is required");
        }

        String normalized = state.trim().toUpperCase();
        if (!normalized.equals("ON") && !normalized.equals("OFF")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state must be ON or OFF");
        }

        SmartDevice device = repo.findByDeviceIdAndUserIdAndActiveTrue(externalDeviceId.trim(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        device.setState(normalized);
        SmartDevice saved = repo.save(device);
        mqttCommandPublisher.publishCommand(saved.getMqttTopic(), normalized);
        return DeviceResponse.from(saved);
    }
}
