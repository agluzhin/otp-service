package ru.agluzhin.service.notification;

public enum NotificationChannelType {
    EMAIL,
    SMS,
    TELEGRAM,
    FILE;

    public NotificationChannel createChannel() {
        return switch (this) {
            case EMAIL -> new EmailNotificationService();
            case SMS -> new SmsNotificationService();
            case TELEGRAM -> new TelegramNotificationService();
            case FILE -> new FileNotificationService();
        };
    }

}
