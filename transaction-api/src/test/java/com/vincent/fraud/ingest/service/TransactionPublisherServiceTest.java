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
        RecordingIngressMetricsService metricsService = new RecordingIngressMetricsService();
        TransactionPublisher publisher = command -> {
            captured.set(command);
            return new PublishedTransaction("tx-1", "msg-1", Instant.parse("2026-06-09T12:00:00Z"));
        };
        TransactionPublisherService service = new TransactionPublisherService(publisher, metricsService);
        PublishTransactionCommand command = new PublishTransactionCommand(
                "acct-1", "merchant-1", "device-1", "10.0.0.1", "USD", new BigDecimal("12.00"),
                Instant.parse("2026-06-09T11:59:00Z")
        );

        PublishedTransaction published = service.publish(command);

        assertThat(captured.get()).isEqualTo(command);
        assertThat(published.transactionId()).isEqualTo("tx-1");
        assertThat(metricsService.incrementCount).isEqualTo(1);
    }

    private static final class RecordingIngressMetricsService extends IngressMetricsService {

        private int incrementCount;

        private RecordingIngressMetricsService() {
            super(new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                    "transaction-api",
                    "pod-1",
                    "node-1");
        }

        @Override
        public void incrementPublishedCount() {
            incrementCount++;
        }
    }
}
