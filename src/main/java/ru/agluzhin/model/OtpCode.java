package ru.agluzhin.model;

import java.time.OffsetDateTime;

public class OtpCode {

    private long id;
    private long userId;
    private String operationId;
    private String code;
    private OtpStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

    public OtpCode() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public OtpStatus getStatus() { return status; }
    public void setStatus(OtpStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

}
