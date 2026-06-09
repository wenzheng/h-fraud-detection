package com.vincent.fraud.ingest.service;

import java.time.Instant;

public record PublishedTransaction(
        String transactionId,
        String queueMessageId,
        Instant queuedAt
) {
}
