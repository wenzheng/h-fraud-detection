package com.vincent.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert.telegram")
public record TelegramProperties(
        boolean enabled,
        String botToken,
        String chatId,
        String baseUrl,
        boolean disableNotification
) {
    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://api.telegram.org" : baseUrl;
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
    }
}
