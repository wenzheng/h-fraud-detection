package com.vincent.fraud.ingest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransactionRequestTest {

    @Test
    void shouldUseProvidedOccurredAtWhenPresent() {
        Instant occurredAt = Instant.parse("2026-06-10T01:02:03Z");
        TransactionRequest request = new TransactionRequest(
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("12.34"),
                occurredAt
        );

        var command = request.toCommand();

        assertThat(command.occurredAt()).isEqualTo(occurredAt);
        assertThat(command.accountId()).isEqualTo("acct-1");
    }

    @Test
    void shouldPopulateOccurredAtWhenMissing() {
        Instant before = Instant.now();
        TransactionRequest request = new TransactionRequest(
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("12.34"),
                null
        );

        var command = request.toCommand();

        assertThat(command.occurredAt()).isBetween(before, Instant.now());
    }
}
