package com.vincent.fraud.ingest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TransactionPublisherServiceTest {

    @Test
    void shouldDelegatePublishing() {
        AtomicReference<PublishTransactionCommand> captured = new AtomicReference<>();
        TransactionPublisher publisher = command -> {
            captured.set(command);
            return new PublishedTransaction("tx-1", "msg-1", Instant.parse("2026-06-09T12:00:00Z"));
        };
        TransactionPublisherService service = new TransactionPublisherService(publisher);
        PublishTransactionCommand command = new PublishTransactionCommand(
                "acct-1", "merchant-1", "device-1", "10.0.0.1", "USD", new BigDecimal("12.00"),
                Instant.parse("2026-06-09T11:59:00Z")
        );

        PublishedTransaction published = service.publish(command);

        assertThat(captured.get()).isEqualTo(command);
        assertThat(published.transactionId()).isEqualTo("tx-1");
    }
}
