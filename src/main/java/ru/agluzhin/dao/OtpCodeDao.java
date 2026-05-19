package ru.agluzhin.dao;

import ru.agluzhin.config.DatabaseConfig;
import ru.agluzhin.model.OtpCode;
import ru.agluzhin.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Optional;

public class OtpCodeDao {

    private static final Logger log = LoggerFactory.getLogger(OtpCodeDao.class);

    private final DataSource dataSource;

    public OtpCodeDao() {
        this.dataSource = DatabaseConfig.getInstance().getDataSource();
    }

    public OtpCodeDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(OtpCode otpCode) {
        String sql = """
                INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, otpCode.getUserId());
            ps.setString(2, otpCode.getOperationId());
            ps.setString(3, otpCode.getCode());
            ps.setString(4, otpCode.getStatus().name());
            ps.setObject(5, otpCode.getCreatedAt());
            ps.setObject(6, otpCode.getExpiresAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    otpCode.setId(keys.getLong(1));
                }
            }
            log.debug("Saved OTP code id={} operationId={}", otpCode.getId(), otpCode.getOperationId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save OTP code", e);
        }
    }

    /**
     * Finds the most recent ACTIVE code for a given user and operation.
     */
    public Optional<OtpCode> findActiveByUserAndOperation(long userId, String operationId) {
        String sql = """
                SELECT id, user_id, operation_id, code, status, created_at, expires_at
                FROM otp_codes
                WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, operationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active OTP code", e);
        }
        return Optional.empty();
    }

    public void updateStatus(long id, OtpStatus status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
            log.debug("OTP code id={} status set to {}", id, status);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update OTP code status id=" + id, e);
        }
    }

    /**
     * Bulk-expires all ACTIVE codes whose expires_at is in the past.
     * Called by the scheduler.
     */
    public int expireOutdatedCodes() {
        String sql = """
                UPDATE otp_codes
                SET status = 'EXPIRED'
                WHERE status = 'ACTIVE' AND expires_at < now()
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int updated = ps.executeUpdate();
            if (updated > 0) {
                log.info("Expired {} outdated OTP code(s)", updated);
            }
            return updated;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to expire outdated OTP codes", e);
        }
    }

    /**
     * Deletes all OTP codes belonging to a user. Called when admin deletes a user.
     */
    public void deleteByUserId(long userId) {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            int affected = ps.executeUpdate();
            log.debug("Deleted {} OTP code(s) for userId={}", affected, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete OTP codes for userId=" + userId, e);
        }
    }

    private OtpCode mapRow(ResultSet rs) throws SQLException {
        OtpCode code = new OtpCode();
        code.setId(rs.getLong("id"));
        code.setUserId(rs.getLong("user_id"));
        code.setOperationId(rs.getString("operation_id"));
        code.setCode(rs.getString("code"));
        code.setStatus(OtpStatus.valueOf(rs.getString("status")));
        code.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        code.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));
        return code;
    }

}
