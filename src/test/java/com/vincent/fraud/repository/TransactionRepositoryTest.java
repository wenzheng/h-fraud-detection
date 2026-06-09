package com.vincent.fraud.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vincent.fraud.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class TransactionRepositoryTest {

    private final TransactionRepository transactionRepository = new TransactionRepository();

    @Test
    void shouldReturnSavedTransaction() {
        Transaction transaction = transaction("tx-1");

        transactionRepository.save(transaction);

        assertThat(transactionRepository.get("tx-1")).isEqualTo(transaction);
    }

    @Test
    void shouldThrowWhenTransactionDoesNotExist() {
        assertThatThrownBy(() -> transactionRepository.get("missing-id"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Transaction not found: missing-id");
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
