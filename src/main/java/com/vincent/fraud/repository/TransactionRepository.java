package com.vincent.fraud.repository;

import com.vincent.fraud.model.Transaction;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    public Transaction save(Transaction transaction) {
        transactions.put(transaction.id(), transaction);
        return transaction;
    }

    public Transaction get(String id) {
        Transaction transaction = transactions.get(id);
        if (transaction == null) {
            throw new NoSuchElementException("Transaction not found: " + id);
        }
        return transaction;
    }

    public List<Transaction> findByAccountWithin(String accountId, Instant fromInclusive) {
        return transactions.values().stream()
                .filter(transaction -> transaction.accountId().equals(accountId))
                .filter(transaction -> !transaction.occurredAt().isBefore(fromInclusive))
                .sorted(Comparator.comparing(Transaction::occurredAt))
                .toList();
    }
}
