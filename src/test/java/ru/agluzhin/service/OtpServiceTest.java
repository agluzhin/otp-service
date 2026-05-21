package ru.agluzhin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.agluzhin.dao.OtpCodeDao;
import ru.agluzhin.dao.OtpConfigDao;
import ru.agluzhin.exception.InvalidOtpException;
import ru.agluzhin.model.OtpCode;
import ru.agluzhin.model.OtpConfig;
import ru.agluzhin.model.OtpStatus;
import ru.agluzhin.service.notification.NotificationChannelType;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpCodeDao otpCodeDao;
    @Mock
    private OtpConfigDao otpConfigDao;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpCodeDao, otpConfigDao);
    }

    // --- generate() ---

    @Test
    void generate_savesCodeWithCorrectLength() {
        when(otpConfigDao.get()).thenReturn(new OtpConfig(6, 300));

        // Use FILE channel so we don't need external services in tests
        otpService.generate(1L, "op-001", NotificationChannelType.FILE, "test");

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeDao).save(captor.capture());

        OtpCode saved = captor.getValue();
        assertThat(saved.getCode()).hasSize(6).matches("\\d+");
        assertThat(saved.getStatus()).isEqualTo(OtpStatus.ACTIVE);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getOperationId()).isEqualTo("op-001");
    }

    @Test
    void generate_throws_whenOperationIdBlank() {
        assertThatThrownBy(() ->
                otpService.generate(1L, "  ", NotificationChannelType.FILE, "dest"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(otpCodeDao, never()).save(any());
    }

    @Test
    void generate_setsExpiresAtFromConfig() {
        OtpConfig cfg = new OtpConfig(4, 120);
        when(otpConfigDao.get()).thenReturn(cfg);

        OffsetDateTime before = OffsetDateTime.now();
        otpService.generate(2L, "op-002", NotificationChannelType.FILE, "dest");
        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeDao).save(captor.capture());

        OffsetDateTime expiresAt = captor.getValue().getExpiresAt();
        assertThat(expiresAt).isAfterOrEqualTo(before.plusSeconds(120));
        assertThat(expiresAt).isBeforeOrEqualTo(after.plusSeconds(120));
    }

    // --- validate() ---

    @Test
    void validate_success_marksCodeUsed() throws Exception {
        OtpCode active = activeCode("123456");
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-001")).thenReturn(Optional.of(active));

        otpService.validate(1L, "op-001", "123456");

        verify(otpCodeDao).updateStatus(active.getId(), OtpStatus.USED);
    }

    @Test
    void validate_throws_whenNoActiveCode() {
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.validate(1L, "op-999", "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("no active code");
    }

    @Test
    void validate_throws_whenCodeExpired() {
        OtpCode expired = activeCode("111111");
        expired.setExpiresAt(OffsetDateTime.now().minusSeconds(1)); // already past

        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-001")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> otpService.validate(1L, "op-001", "111111"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("expired");

        verify(otpCodeDao).updateStatus(expired.getId(), OtpStatus.EXPIRED);
    }

    @Test
    void validate_throws_whenCodeDoesNotMatch() {
        OtpCode active = activeCode("999999");
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-001")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> otpService.validate(1L, "op-001", "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("does not match");

        verify(otpCodeDao, never()).updateStatus(anyLong(), eq(OtpStatus.USED));
    }

    // --- helpers ---

    private OtpCode activeCode(String code) {
        OtpCode otp = new OtpCode();
        otp.setId(1L);
        otp.setUserId(1L);
        otp.setOperationId("op-001");
        otp.setCode(code);
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setCreatedAt(OffsetDateTime.now());
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        return otp;
    }

}
