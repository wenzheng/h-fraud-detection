package com.vincent.fraud.service;

import java.math.BigDecimal;
import java.time.Instant;

public record IngestTransactionCommand(
        String accountId,
        String merchantId,
        String deviceId,
        String ipAddress,
        String currency,
        BigDecimal amount,
        Instant occurredAt
) {
}
