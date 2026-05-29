package com.diwan.smarthome.dto;

import com.diwan.smarthome.entity.SmartDevice;
import java.time.LocalDateTime;

public class DeviceResponse {
    public Long          id;
    public String        deviceId;
    public String        name;
    public String        mqttTopic;
    public String        state;
    public boolean       active;
    public LocalDateTime createdAt;

    public static DeviceResponse from(SmartDevice d) {
        DeviceResponse r = new DeviceResponse();
        r.id         = d.getId();
        r.deviceId   = d.getDeviceId();
        r.name       = d.getName();
        r.mqttTopic  = d.getMqttTopic();
        r.state      = d.getState();
        r.active     = d.isActive();
        r.createdAt  = d.getCreatedAt();
        return r;
    }
}
