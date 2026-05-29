package com.diwan.smarthome.dto;

public class DeviceStateRequest {
    private String state;  // ON | OFF | UNKNOWN

    public String getState() { return state; }
    public void setState(String v) { state = v; }
}
