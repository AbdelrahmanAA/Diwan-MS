package com.diwan.logging.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_logs")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;

    @Column(length = 512)
    private String path;

    private Integer statusCode;
    private Long durationMs;
    private Long userId;
    private String sourceService;
    private String targetService;

    @Column(length = 64)
    private String clientIp;

    @Column(length = 1024)
    private String errorMessage;

    private LocalDateTime requestTime;

    // Getters & Setters
    public Long getId()                      { return id; }
    public String getMethod()                { return method; }
    public void setMethod(String v)          { this.method = v; }
    public String getPath()                  { return path; }
    public void setPath(String v)            { this.path = v; }
    public Integer getStatusCode()           { return statusCode; }
    public void setStatusCode(Integer v)     { this.statusCode = v; }
    public Long getDurationMs()              { return durationMs; }
    public void setDurationMs(Long v)        { this.durationMs = v; }
    public Long getUserId()                  { return userId; }
    public void setUserId(Long v)            { this.userId = v; }
    public String getSourceService()         { return sourceService; }
    public void setSourceService(String v)   { this.sourceService = v; }
    public String getTargetService()         { return targetService; }
    public void setTargetService(String v)   { this.targetService = v; }
    public String getClientIp()              { return clientIp; }
    public void setClientIp(String v)        { this.clientIp = v; }
    public String getErrorMessage()          { return errorMessage; }
    public void setErrorMessage(String v)    { this.errorMessage = v; }
    public LocalDateTime getRequestTime()    { return requestTime; }
    public void setRequestTime(LocalDateTime v) { this.requestTime = v; }
}
