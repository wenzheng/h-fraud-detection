package com.vincent.fraud.alert.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlertMetricsService {

    static final String METRIC_NAME = "fraud_alerts_handled_total";

    private final Counter handledCounter;

    public AlertMetricsService(
            MeterRegistry meterRegistry,
            @Value("${spring.application.name}") String applicationName,
            @Value("${HOSTNAME:unknown}") String podName,
            @Value("${NODE_NAME:unknown}") String nodeName
    ) {
        this.handledCounter = Counter.builder(METRIC_NAME)
                .description("Total alerts handled by the alert routing service")
                .tag("application", applicationName)
                .tag("pod", podName)
                .tag("node", nodeName)
                .register(meterRegistry);
    }

    public void incrementHandledCount() {
        handledCounter.increment();
    }
}
