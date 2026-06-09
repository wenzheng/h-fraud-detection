package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.shared.model.AlertEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AlertServiceTest {

    @Test
    void shouldPublishAlertEventToQueue() {
        AtomicReference<AlertEvent> alertRef = new AtomicReference<>();
        AlertPublisher publisher = alertEvent -> {
            alertRef.set(alertEvent);
            return "msg-1";
        };
        AlertService alertService = new AlertService(publisher, new AlertSeverityResolver());
        var transactionEvent = transactionEvent();

        alertService.raise(transactionEvent, List.of("Amount exceeded threshold"));

        assertThat(alertRef.get()).isNotNull();
        assertThat(alertRef.get().transactionId()).isEqualTo(transactionEvent.transactionId());
        assertThat(alertRef.get().merchantId()).isEqualTo(transactionEvent.merchantId());
        assertThat(alertRef.get().severity()).isEqualTo(AlertEvent.Severity.HIGH);
    }

    @Test
    void shouldAssignMediumSeverityForSingleMerchantReason() {
        AtomicReference<AlertEvent> alertRef = new AtomicReference<>();
        AlertPublisher publisher = alertEvent -> {
            alertRef.set(alertEvent);
            return "msg-2";
        };
        AlertService alertService = new AlertService(publisher, new AlertSeverityResolver());

        alertService.raise(transactionEvent(), List.of("Merchant is on the suspicious merchant list"));

        assertThat(alertRef.get().severity()).isEqualTo(AlertEvent.Severity.MEDIUM);
    }

    @Test
    void shouldAssignLowSeverityForGenericReason() {
        AtomicReference<AlertEvent> alertRef = new AtomicReference<>();
        AlertPublisher publisher = alertEvent -> {
            alertRef.set(alertEvent);
            return "msg-3";
        };
        AlertService alertService = new AlertService(publisher, new AlertSeverityResolver());

        alertService.raise(transactionEvent(), List.of("Velocity anomaly observed"));

        assertThat(alertRef.get().severity()).isEqualTo(AlertEvent.Severity.LOW);
    }

    private com.vincent.fraud.shared.model.TransactionEvent transactionEvent() {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");
        return new com.vincent.fraud.shared.model.TransactionEvent(
                "tx-1",
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("100.00"),
                now,
                now
        );
    }
}
