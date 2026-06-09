package com.vincent.fraud.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.consumer")
public record ConsumerProperties(
        int pollerConcurrency,
        int batchSize,
        int waitSeconds
) {
}
