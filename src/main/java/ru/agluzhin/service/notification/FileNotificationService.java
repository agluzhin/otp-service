package ru.agluzhin.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(FileNotificationService.class);
    private static final Path OUTPUT_FILE = Paths.get("otp_codes.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void send(String destination, String code) {
        String line = String.format("[%s] destination=%s code=%s%n",
                LocalDateTime.now().format(FORMATTER), destination, code);
        try {
            Files.writeString(OUTPUT_FILE, line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            log.info("OTP saved to file for destination={}", destination);
        } catch (IOException e) {
            log.error("Failed to write OTP to file: {}", e.getMessage(), e);
            throw new RuntimeException("File delivery failed", e);
        }
    }

}
