package com.diwan.smarthome.dto;

public class RegisterDeviceRequest {
    private String deviceId;  // from QR code
    private String name;      // user-provided label

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String v) { deviceId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
}
