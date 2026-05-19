package ru.agluzhin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is == null) {
                throw new IllegalStateException("application.properties not found in classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public int getServerPort() {
        return Integer.parseInt(props.getProperty("server.port", "8080"));
    }

    public String getDbUrl() {
        return props.getProperty("db.url");
    }

    public String getDbUsername() {
        return props.getProperty("db.username");
    }

    public String getDbPassword() {
        return props.getProperty("db.password");
    }

    public int getDbPoolSize() {
        return Integer.parseInt(props.getProperty("db.pool.size", "10"));
    }

    public String getJwtSecret() {
        return props.getProperty("jwt.secret");
    }

    public long getJwtExpirationSeconds() {
        return Long.parseLong(props.getProperty("jwt.expiration.seconds", "3600"));
    }

    public long getSchedulerIntervalSeconds() {
        return Long.parseLong(props.getProperty("scheduler.interval.seconds", "60"));
    }

}
