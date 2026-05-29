package com.diwan.users.kafka;
public class UserInvalidatedEvent {
    private Long userId;
    private String reason; // "DELETED" or "DEACTIVATED"
    public UserInvalidatedEvent(){}
    public UserInvalidatedEvent(Long userId, String reason){this.userId=userId;this.reason=reason;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
}