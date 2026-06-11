package com.vincent.fraud.ingest.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IngressMetricsService {

    static final String METRIC_NAME = "fraud_ingress_transactions_total";

    private final Counter publishedCounter;

    public IngressMetricsService(
            MeterRegistry meterRegistry,
            @Value("${spring.application.name}") String applicationName,
            @Value("${HOSTNAME:unknown}") String podName,
            @Value("${NODE_NAME:unknown}") String nodeName
    ) {
        this.publishedCounter = Counter.builder(METRIC_NAME)
                .description("Total accepted transactions published to the queue")
                .tag("application", applicationName)
                .tag("pod", podName)
                .tag("node", nodeName)
                .register(meterRegistry);
    }

    public void incrementPublishedCount() {
        publishedCounter.increment();
    }
}
