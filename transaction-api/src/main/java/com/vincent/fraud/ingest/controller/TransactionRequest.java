package com.vincent.fraud.ingest.controller;

import com.vincent.fraud.ingest.service.PublishTransactionCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
        @NotBlank String accountId,
        @NotBlank String merchantId,
        @NotBlank String deviceId,
        @NotBlank String ipAddress,
        @NotBlank String currency,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        Instant occurredAt
) {
    public PublishTransactionCommand toCommand() {
        return new PublishTransactionCommand(
                accountId,
                merchantId,
                deviceId,
                ipAddress,
                currency,
                amount,
                occurredAt == null ? Instant.now() : occurredAt
        );
    }
}
