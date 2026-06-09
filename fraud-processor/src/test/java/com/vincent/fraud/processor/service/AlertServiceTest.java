package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.shared.model.AlertEvent;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AlertServiceTest {

    @Test
    void shouldNotifyConfiguredNotifiers() {
        AtomicReference<AlertEvent> alertRef = new AtomicReference<>();
        AtomicReference<TransactionEvent> transactionRef = new AtomicReference<>();
        AlertNotifier notifier = (alertEvent, transactionEvent) -> {
            alertRef.set(alertEvent);
            transactionRef.set(transactionEvent);
        };
        AlertService alertService = new AlertService(List.of(notifier));
        TransactionEvent transactionEvent = transactionEvent();

        alertService.raise(transactionEvent, List.of("Amount exceeded threshold"));

        assertThat(alertRef.get()).isNotNull();
        assertThat(alertRef.get().transactionId()).isEqualTo(transactionEvent.transactionId());
        assertThat(transactionRef.get()).isEqualTo(transactionEvent);
    }

    private TransactionEvent transactionEvent() {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");
        return new TransactionEvent(
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
