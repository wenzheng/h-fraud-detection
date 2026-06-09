package com.vincent.fraud.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String id,
        String accountId,
        String merchantId,
        String deviceId,
        String ipAddress,
        String currency,
        BigDecimal amount,
        Instant occurredAt,
        Instant receivedAt,
        Status status
) {
    public enum Status {
        RECEIVED,
        APPROVED,
        FLAGGED
    }

    public Transaction withStatus(Status updatedStatus) {
        return new Transaction(
                id,
                accountId,
                merchantId,
                deviceId,
                ipAddress,
                currency,
                amount,
                occurredAt,
                receivedAt,
                updatedStatus
        );
    }
}
