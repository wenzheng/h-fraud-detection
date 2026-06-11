package com.vincent.fraud.processor.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProcessingMetricsService {

    static final String METRIC_NAME = "fraud_data_points_processed_total";

    private final Counter processedCounter;

    public ProcessingMetricsService(
            MeterRegistry meterRegistry,
            @Value("${spring.application.name}") String applicationName,
            @Value("${HOSTNAME:unknown}") String podName,
            @Value("${NODE_NAME:unknown}") String nodeName
    ) {
        this.processedCounter = Counter.builder(METRIC_NAME)
                .description("Total processed fraud data points")
                .tag("application", applicationName)
                .tag("pod", podName)
                .tag("node", nodeName)
                .register(meterRegistry);
    }

    public void incrementProcessedCount() {
        processedCounter.increment();
    }
}
