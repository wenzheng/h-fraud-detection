package com.vincent.fraud.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mns")
public record MnsProperties(
        String endpoint,
        String queueName,
        String accessKeyId,
        String accessKeySecret
) {
}
