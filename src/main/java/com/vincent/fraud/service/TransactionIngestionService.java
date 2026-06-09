package com.vincent.fraud.service;

import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.queue.TransactionQueue;
import com.vincent.fraud.repository.TransactionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionIngestionService {

    private final TransactionRepository transactionRepository;
    private final TransactionQueue transactionQueue;

    public TransactionIngestionService(TransactionRepository transactionRepository, TransactionQueue transactionQueue) {
        this.transactionRepository = transactionRepository;
        this.transactionQueue = transactionQueue;
    }

    public Transaction ingest(IngestTransactionCommand command) {
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                command.accountId(),
                command.merchantId(),
                command.deviceId(),
                command.ipAddress(),
                command.currency(),
                command.amount(),
                command.occurredAt(),
                Instant.now(),
                Transaction.Status.RECEIVED
        );
        transactionRepository.save(transaction);
        transactionQueue.publish(transaction);
        return transaction;
    }
}
