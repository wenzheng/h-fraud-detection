package com.vincent.fraud.shared.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionEvent(
        String transactionId,
        String accountId,
        String merchantId,
        String deviceId,
        String ipAddress,
        String currency,
        BigDecimal amount,
        Instant occurredAt,
        Instant receivedAt
) {
}
