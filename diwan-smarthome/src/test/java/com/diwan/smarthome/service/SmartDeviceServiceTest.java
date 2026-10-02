package com.diwan.smarthome.service;

import com.diwan.smarthome.dto.DeviceResponse;
import com.diwan.smarthome.dto.RegisterDeviceRequest;
import com.diwan.smarthome.entity.SmartDevice;
import com.diwan.smarthome.repository.SmartDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartDeviceServiceTest {

    private final SmartDeviceRepository repo = mock(SmartDeviceRepository.class);
    private final MqttCommandPublisher mqtt = mock(MqttCommandPublisher.class);
    private final SmartDeviceService service = new SmartDeviceService(repo, mqtt);

    private static SmartDevice device(Long userId, String deviceId) {
        SmartDevice d = new SmartDevice();
        d.setUserId(userId);
        d.setDeviceId(deviceId);
        d.setName("Lamp");
        d.setMqttTopic("myhome/SmartLamp_" + deviceId);
        d.setState("OFF");
        return d;
    }

    private static RegisterDeviceRequest request(String deviceId, String name) {
        RegisterDeviceRequest r = new RegisterDeviceRequest();
        r.setDeviceId(deviceId);
        r.setName(name);
        return r;
    }

    private static HttpStatus statusOf(Runnable action) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, action::run).getStatusCode();
    }

    @Test
    void registerBuildsTheMqttTopicFromTheDeviceId() {
        when(repo.findByDeviceIdAndUserId("lamp1", 7L)).thenReturn(Optional.empty());
        when(repo.save(any(SmartDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceResponse res = service.register(7L, request(" lamp1 ", " Kitchen "));

        assertEquals("lamp1", res.deviceId);
        assertEquals("Kitchen", res.name);
        assertEquals("myhome/SmartLamp_lamp1", res.mqttTopic);
    }

    @Test
    void registerValidatesInputAndRejectsDuplicates() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.register(7L, request("", "x"))));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.register(7L, request("a", " "))));
        when(repo.findByDeviceIdAndUserId("lamp1", 7L)).thenReturn(Optional.of(device(7L, "lamp1")));
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.register(7L, request("lamp1", "x"))));
        verify(repo, never()).save(any());
    }

    @Test
    void aUserCannotRemoveOrRenameSomeoneElsesDevice() {
        when(repo.findByIdAndUserId(5L, 7L)).thenReturn(Optional.empty()); // belongs to another user

        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.removeDevice(7L, 5L)));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.renameDevice(7L, 5L, "new")));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.updateState(7L, 5L, "ON")));
        verify(repo, never()).save(any());
    }

    @Test
    void removeIsASoftDelete() {
        SmartDevice d = device(7L, "lamp1");
        when(repo.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(d));
        service.removeDevice(7L, 5L);
        assertFalse(d.isActive());
        verify(repo).save(d);
    }

    @Test
    void controlPersistsTheStateAndPublishesTheNormalizedCommand() {
        SmartDevice d = device(7L, "lamp1");
        when(repo.findByDeviceIdAndUserIdAndActiveTrue("lamp1", 7L)).thenReturn(Optional.of(d));
        when(repo.save(d)).thenReturn(d);

        DeviceResponse res = service.controlByExternalDeviceId(7L, " lamp1 ", " on ");

        assertEquals("ON", res.state);
        verify(mqtt).publishCommand("myhome/SmartLamp_lamp1", "ON");
    }

    @Test
    void controlRejectsUnknownStatesAndForeignDevicesWithoutSendingAnything() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.controlByExternalDeviceId(7L, "lamp1", "UNKNOWN")));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.controlByExternalDeviceId(7L, "lamp1", "dim")));
        when(repo.findByDeviceIdAndUserIdAndActiveTrue("lamp1", 7L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.controlByExternalDeviceId(7L, "lamp1", "ON")));
        verify(mqtt, never()).publishCommand(anyString(), anyString());
    }
}
