package ru.agluzhin.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class OtpCode {

    private long id;
    private long userId;
    private String operationId;
    private String code;
    private OtpStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

}
