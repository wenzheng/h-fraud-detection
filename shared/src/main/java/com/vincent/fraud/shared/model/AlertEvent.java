package com.vincent.fraud.shared.model;

import java.time.Instant;
import java.util.List;

public record AlertEvent(
        String alertId,
        String transactionId,
        String accountId,
        List<String> reasons,
        Severity severity,
        Instant detectedAt
) {
    public enum Severity {
        MEDIUM,
        HIGH
    }
}
