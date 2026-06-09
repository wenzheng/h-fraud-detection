package com.vincent.fraud.queue;

import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.service.FraudAnalysisOrchestrator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryTransactionQueue implements TransactionQueue {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTransactionQueue.class);

    private final BlockingQueue<Transaction> queue = new LinkedBlockingQueue<>();
    private final FraudAnalysisOrchestrator orchestrator;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public InMemoryTransactionQueue(FraudAnalysisOrchestrator orchestrator, ExecutorService executorService) {
        this.orchestrator = orchestrator;
        this.executorService = executorService;
    }

    @PostConstruct
    void startConsumer() {
        executorService.submit(() -> {
            while (running) {
                try {
                    Transaction transaction = queue.take();
                    orchestrator.analyze(transaction);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (Exception exception) {
                    log.error("Failed to process transaction from queue", exception);
                }
            }
        });
    }

    @PreDestroy
    void stopConsumer() {
        running = false;
        executorService.shutdownNow();
    }

    @Override
    public void publish(Transaction transaction) {
        queue.offer(transaction);
    }
}
