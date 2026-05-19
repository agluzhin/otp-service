package ru.agluzhin.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String botToken;
    private final String chatId;
    private final String apiBaseUrl;
    private final HttpClient httpClient;

    public TelegramNotificationService() {
        Properties cfg = loadConfig();
        this.botToken   = cfg.getProperty("telegram.bot.token");
        this.chatId     = cfg.getProperty("telegram.chat.id");
        this.apiBaseUrl = cfg.getProperty("telegram.api.url");
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void send(String destination, String code) {
        // 'destination' is ignored here; message goes to the configured chatId.
        // In a multi-user scenario, destination would be a per-user chatId stored in the DB.
        String text    = URLEncoder.encode("Your OTP code: " + code, StandardCharsets.UTF_8);
        String url     = apiBaseUrl + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + text;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Telegram API error, status={} body={}", response.statusCode(), response.body());
                throw new RuntimeException("Telegram delivery failed, status=" + response.statusCode());
            }
            log.info("OTP sent via Telegram to chatId={}", chatId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Telegram request interrupted", e);
        } catch (IOException e) {
            log.error("Telegram IO error: {}", e.getMessage(), e);
            throw new RuntimeException("Telegram delivery failed", e);
        }
    }

    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("telegram.properties")) {
            if (is == null) throw new IllegalStateException("telegram.properties not found");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load telegram.properties", e);
        }
    }

}
