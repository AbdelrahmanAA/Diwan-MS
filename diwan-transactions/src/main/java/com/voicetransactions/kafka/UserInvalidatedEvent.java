package com.voicetransactions.kafka;
public class UserInvalidatedEvent {
    private Long userId; private String reason;
    public UserInvalidatedEvent(){}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
}