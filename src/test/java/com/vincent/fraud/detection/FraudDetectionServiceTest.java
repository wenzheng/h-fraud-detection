package com.vincent.fraud.detection;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.config.FraudProperties;
import com.vincent.fraud.model.FraudDecision;
import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudDetectionServiceTest {

    private FraudDetectionService fraudDetectionService;
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        FraudProperties properties = new FraudProperties(
                new BigDecimal("1000"),
                3,
                Duration.ofMinutes(2),
                List.of("acct-watch")
        );
        transactionRepository = new TransactionRepository();
        fraudDetectionService = new FraudDetectionService(List.of(
                new AmountThresholdRule(properties),
                new SuspiciousAccountRule(properties),
                new VelocityRule(properties, transactionRepository)
        ));
    }

    @Test
    void shouldFlagAmountAboveThreshold() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-normal", "1500", Instant.parse("2026-06-09T12:00:00Z")));

        assertThat(decision.fraudulent()).isTrue();
        assertThat(decision.reasons()).contains("Amount exceeded threshold");
    }

    @Test
    void shouldFlagSuspiciousAccount() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-watch", "20", Instant.parse("2026-06-09T12:00:00Z")));

        assertThat(decision.fraudulent()).isTrue();
        assertThat(decision.reasons()).contains("Account is on the suspicious watchlist");
    }

    @Test
    void shouldFlagVelocityBurst() {
        Instant base = Instant.parse("2026-06-09T12:00:00Z");
        transactionRepository.save(transaction("acct-fast", "20", base.minusSeconds(90)));
        transactionRepository.save(transaction("acct-fast", "30", base.minusSeconds(30)));
        transactionRepository.save(transaction("acct-fast", "40", base.minusSeconds(10)));

        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-fast", "50", base));

        assertThat(decision.fraudulent()).isTrue();
        assertThat(decision.reasons()).contains("Velocity rule triggered for account");
    }

    @Test
    void shouldApproveLegitimateTransaction() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-good", "90", Instant.parse("2026-06-09T12:00:00Z")));

        assertThat(decision.fraudulent()).isFalse();
        assertThat(decision.reasons()).isEmpty();
    }

    private Transaction transaction(String accountId, String amount, Instant occurredAt) {
        return new Transaction(
                "tx-" + accountId + "-" + amount,
                accountId,
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal(amount),
                occurredAt,
                occurredAt,
                Transaction.Status.RECEIVED
        );
    }
}
