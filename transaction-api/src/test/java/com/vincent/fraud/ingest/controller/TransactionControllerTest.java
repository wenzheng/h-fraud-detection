package com.vincent.fraud.ingest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.ingest.service.PublishedTransaction;
import com.vincent.fraud.ingest.service.TransactionPublisherService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransactionControllerTest {

    @Test
    void shouldReturnQueuedResponse() {
        TransactionPublisherService publisherService = new TransactionPublisherService(
                command -> new PublishedTransaction("tx-1", "msg-1", Instant.parse("2026-06-10T01:02:04Z"))
        );
        TransactionController controller = new TransactionController(publisherService);
        TransactionRequest request = new TransactionRequest(
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("99.99"),
                Instant.parse("2026-06-10T01:02:03Z")
        );

        var response = controller.submit(request);

        assertThat(response.transactionId()).isEqualTo("tx-1");
        assertThat(response.queueMessageId()).isEqualTo("msg-1");
        assertThat(response.status()).isEqualTo("QUEUED");
    }
}
