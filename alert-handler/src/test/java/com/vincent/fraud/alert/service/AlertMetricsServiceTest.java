package com.vincent.fraud.alert.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AlertMetricsServiceTest {

    @Test
    void shouldRegisterTaggedCounterAndIncrementIt() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AlertMetricsService service = new AlertMetricsService(
                meterRegistry,
                "alert-handler",
                "alert-handler-pod-1",
                "node-a"
        );

        service.incrementHandledCount();

        Counter counter = meterRegistry.find(AlertMetricsService.METRIC_NAME)
                .tag("application", "alert-handler")
                .tag("pod", "alert-handler-pod-1")
                .tag("node", "node-a")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0d);
    }
}
