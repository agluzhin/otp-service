package ru.agluzhin.service.notification;

public interface NotificationChannel {

    void send(String destination, String code);
    
}
