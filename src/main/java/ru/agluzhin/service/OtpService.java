package ru.agluzhin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.agluzhin.dao.OtpCodeDao;
import ru.agluzhin.dao.OtpConfigDao;
import ru.agluzhin.exception.InvalidOtpException;
import ru.agluzhin.model.OtpCode;
import ru.agluzhin.model.OtpConfig;
import ru.agluzhin.model.OtpStatus;
import ru.agluzhin.service.notification.NotificationChannel;
import ru.agluzhin.service.notification.NotificationChannelType;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;

    public OtpService() {
        this(new OtpCodeDao(), new OtpConfigDao());
    }

    public OtpService(OtpCodeDao otpCodeDao, OtpConfigDao otpConfigDao) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
    }

    /**
     * Generates an OTP code, persists it, and delivers it through the chosen channel.
     *
     * @param userId      authenticated user
     * @param operationId arbitrary operation identifier supplied by the caller
     * @param channelType delivery channel
     * @param destination channel-specific address (email, phone, etc.)
     */
    public void generate(long userId, String operationId,
                         NotificationChannelType channelType, String destination) {

        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }

        OtpConfig config = otpConfigDao.get();
        String code = generateNumericCode(config.getCodeLength());

        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setOperationId(operationId);
        otpCode.setCode(code);
        otpCode.setStatus(OtpStatus.ACTIVE);
        otpCode.setCreatedAt(OffsetDateTime.now());
        otpCode.setExpiresAt(OffsetDateTime.now().plusSeconds(config.getTtlSeconds()));

        otpCodeDao.save(otpCode);
        log.info("OTP generated: userId={} operationId={} channel={}", userId, operationId, channelType);

        NotificationChannel channel = channelType.createChannel();
        channel.send(destination, code);
    }

    /**
     * Validates the OTP code submitted by the user.
     * On success marks it USED; on any failure throws {@link InvalidOtpException}.
     */
    public void validate(long userId, String operationId, String submittedCode) throws InvalidOtpException {
        OtpCode otpCode = otpCodeDao
                .findActiveByUserAndOperation(userId, operationId)
                .orElseThrow(() -> new InvalidOtpException("no active code found for this operation"));

        if (OffsetDateTime.now().isAfter(otpCode.getExpiresAt())) {
            otpCodeDao.updateStatus(otpCode.getId(), OtpStatus.EXPIRED);
            log.warn("OTP expired at validation: userId={} operationId={}", userId, operationId);
            throw new InvalidOtpException("code has expired");
        }

        if (!otpCode.getCode().equals(submittedCode)) {
            log.warn("OTP wrong code: userId={} operationId={}", userId, operationId);
            throw new InvalidOtpException("code does not match");
        }

        otpCodeDao.updateStatus(otpCode.getId(), OtpStatus.USED);
        log.info("OTP validated successfully: userId={} operationId={}", userId, operationId);
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

}
