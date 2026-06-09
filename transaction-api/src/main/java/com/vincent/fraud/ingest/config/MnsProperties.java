package com.vincent.fraud.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mns")
public record MnsProperties(
        String endpoint,
        String transactionQueueName,
        String accessKeyId,
        String accessKeySecret
) {
}
