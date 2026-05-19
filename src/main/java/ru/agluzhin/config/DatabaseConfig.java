package ru.agluzhin.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();
    private final HikariDataSource dataSource;

    private DatabaseConfig() {
        AppConfig cfg = AppConfig.getInstance();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(cfg.getDbUrl());
        hikariConfig.setUsername(cfg.getDbUsername());
        hikariConfig.setPassword(cfg.getDbPassword());
        hikariConfig.setMaximumPoolSize(cfg.getDbPoolSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30_000);
        hikariConfig.setIdleTimeout(600_000);
        hikariConfig.setMaxLifetime(1_800_000);
        hikariConfig.setPoolName("OtpServicePool");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
