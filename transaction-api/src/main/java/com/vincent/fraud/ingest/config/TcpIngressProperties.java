package com.vincent.fraud.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tcp")
public record TcpIngressProperties(
        int port,
        int workerThreads
) {
}
