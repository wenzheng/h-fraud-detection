package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.shared.model.AlertEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertSeverityResolverTest {

    private final AlertSeverityResolver resolver = new AlertSeverityResolver();

    @Test
    void shouldReturnHighForMultipleReasons() {
        assertThat(resolver.resolve(List.of("a", "b"))).isEqualTo(AlertEvent.Severity.HIGH);
    }

    @Test
    void shouldReturnHighForThresholdReason() {
        assertThat(resolver.resolve(List.of("Amount exceeded threshold"))).isEqualTo(AlertEvent.Severity.HIGH);
    }

    @Test
    void shouldReturnMediumForMerchantReason() {
        assertThat(resolver.resolve(List.of("Merchant is on the suspicious merchant list")))
                .isEqualTo(AlertEvent.Severity.MEDIUM);
    }

    @Test
    void shouldReturnLowForGenericReason() {
        assertThat(resolver.resolve(List.of("Velocity anomaly observed"))).isEqualTo(AlertEvent.Severity.LOW);
    }
}
