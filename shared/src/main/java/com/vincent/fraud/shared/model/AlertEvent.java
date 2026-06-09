package com.vincent.fraud.shared.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AlertEvent(
        String alertId,
        String transactionId,
        String accountId,
        String merchantId,
        String deviceId,
        String ipAddress,
        BigDecimal amount,
        String currency,
        List<String> reasons,
        Severity severity,
        Instant occurredAt,
        Instant detectedAt
) {
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}
