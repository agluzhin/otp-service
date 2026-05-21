package ru.agluzhin.dao;

import ru.agluzhin.config.DatabaseConfig;
import ru.agluzhin.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;

public class OtpConfigDao {

    private static final Logger log = LoggerFactory.getLogger(OtpConfigDao.class);

    private final DataSource dataSource;

    public OtpConfigDao() {
        this.dataSource = DatabaseConfig.getINSTANCE().getDataSource();
    }

    public OtpConfigDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public OtpConfig get() {
        String sql = "SELECT code_length, ttl_seconds FROM otp_config WHERE id = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new OtpConfig(rs.getInt("code_length"), rs.getInt("ttl_seconds"));
            }
            throw new IllegalStateException("otp_config table has no seed row");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load OTP config", e);
        }
    }

    public void update(OtpConfig config) {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ? WHERE id = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, config.getCodeLength());
            ps.setInt(2, config.getTtlSeconds());
            ps.executeUpdate();
            log.info("OTP config updated: codeLength={} ttlSeconds={}", config.getCodeLength(), config.getTtlSeconds());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update OTP config", e);
        }
    }

}
