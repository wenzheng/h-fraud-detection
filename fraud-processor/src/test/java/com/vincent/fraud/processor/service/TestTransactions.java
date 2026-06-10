package com.vincent.fraud.processor.service;

import com.vincent.fraud.shared.model.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;

final class TestTransactions {

    private TestTransactions() {
    }

    static TransactionEvent transaction() {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        return new TransactionEvent(
                "tx-1",
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("42.00"),
                now,
                now
        );
    }
}
