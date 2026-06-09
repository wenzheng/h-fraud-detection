package com.vincent.fraud.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mns")
public record MnsProperties(
        String endpoint,
        String alertQueueName,
        String accessKeyId,
        String accessKeySecret
) {
}
