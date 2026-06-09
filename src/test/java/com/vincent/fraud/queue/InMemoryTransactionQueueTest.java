package com.vincent.fraud.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.service.FraudAnalysisOrchestrator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryTransactionQueueTest {

    @Test
    void shouldContinueConsumingWhenAnalyzeThrowsException() throws InterruptedException {
        AtomicInteger analyzeAttempts = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        FraudAnalysisOrchestrator orchestrator = new FraudAnalysisOrchestrator(null, null, null) {
            @Override
            public void analyze(Transaction transaction) {
                int attempt = analyzeAttempts.incrementAndGet();
                latch.countDown();
                if (attempt == 1) {
                    throw new RuntimeException("boom");
                }
            }
        };
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        InMemoryTransactionQueue queue = new InMemoryTransactionQueue(orchestrator, executorService);

        queue.startConsumer();
        try {
            queue.publish(transaction("tx-1"));
            queue.publish(transaction("tx-2"));

            await().atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isTrue());

            assertThat(analyzeAttempts.get()).isEqualTo(2);
        } finally {
            queue.stopConsumer();
        }
    }

    private Transaction transaction(String id) {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");
        return new Transaction(
                id,
                "acct-test",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("100.00"),
                now,
                now,
                Transaction.Status.RECEIVED
        );
    }
}
