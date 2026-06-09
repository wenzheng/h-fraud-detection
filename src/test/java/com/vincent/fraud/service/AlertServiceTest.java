package com.vincent.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.model.Alert;
import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.repository.AlertRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AlertServiceTest {

    @Test
    void shouldPersistAlertAndNotifyConfiguredNotifiers() {
        AlertRepository alertRepository = new AlertRepository();
        AtomicReference<Alert> notifiedAlert = new AtomicReference<>();
        AtomicReference<Transaction> notifiedTransaction = new AtomicReference<>();
        AlertNotifier notifier = (alert, transaction) -> {
            notifiedAlert.set(alert);
            notifiedTransaction.set(transaction);
        };
        AlertService alertService = new AlertService(alertRepository, List.of(notifier));
        Transaction transaction = transaction();

        alertService.raise(transaction, List.of("Amount exceeded threshold"));

        List<Alert> storedAlerts = alertRepository.findAll();
        assertThat(storedAlerts).hasSize(1);
        assertThat(storedAlerts.get(0).transactionId()).isEqualTo(transaction.id());
        assertThat(notifiedAlert.get()).isEqualTo(storedAlerts.get(0));
        assertThat(notifiedTransaction.get()).isEqualTo(transaction);
    }

    private Transaction transaction() {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");
        return new Transaction(
                "tx-1",
                "acct-test",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("100.00"),
                now,
                now,
                Transaction.Status.FLAGGED
        );
    }
}
