package com.vincent.fraud.ingest.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class IngressMetricsServiceTest {

    @Test
    void shouldRegisterTaggedCounterAndIncrementIt() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        IngressMetricsService service = new IngressMetricsService(
                meterRegistry,
                "transaction-api",
                "transaction-api-pod-1",
                "node-a"
        );

        service.incrementPublishedCount();

        Counter counter = meterRegistry.find(IngressMetricsService.METRIC_NAME)
                .tag("application", "transaction-api")
                .tag("pod", "transaction-api-pod-1")
                .tag("node", "node-a")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0d);
    }
}
