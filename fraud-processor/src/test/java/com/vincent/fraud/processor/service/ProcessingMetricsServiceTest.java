package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ProcessingMetricsServiceTest {

    @Test
    void shouldRegisterTaggedCounterAndIncrementIt() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ProcessingMetricsService service = new ProcessingMetricsService(
                meterRegistry,
                "fraud-processor",
                "fraud-processor-pod-1",
                "node-a"
        );

        service.incrementProcessedCount();

        Counter counter = meterRegistry.find(ProcessingMetricsService.METRIC_NAME)
                .tag("application", "fraud-processor")
                .tag("pod", "fraud-processor-pod-1")
                .tag("node", "node-a")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0d);
    }
}
