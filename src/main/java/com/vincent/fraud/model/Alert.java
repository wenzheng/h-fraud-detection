package com.vincent.fraud.model;

import java.time.Instant;
import java.util.List;

public record Alert(
        String id,
        String transactionId,
        String accountId,
        List<String> reasons,
        Severity severity,
        Instant createdAt
) {
    public enum Severity {
        MEDIUM,
        HIGH
    }
}
