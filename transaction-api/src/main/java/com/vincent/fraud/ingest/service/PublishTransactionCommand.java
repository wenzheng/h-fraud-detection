package com.vincent.fraud.ingest.service;

import java.math.BigDecimal;
import java.time.Instant;

public record PublishTransactionCommand(
        String accountId,
        String merchantId,
        String deviceId,
        String ipAddress,
        String currency,
        BigDecimal amount,
        Instant occurredAt
) {
}
