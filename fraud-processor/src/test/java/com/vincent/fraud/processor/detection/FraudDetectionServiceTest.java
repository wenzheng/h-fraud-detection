package com.vincent.fraud.processor.detection;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.processor.config.FraudRulesProperties;
import com.vincent.fraud.shared.model.FraudDecision;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudDetectionServiceTest {

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        FraudRulesProperties properties = new FraudRulesProperties(
                new BigDecimal("1000"),
                List.of("acct-watch"),
                List.of("merchant-risk")
        );
        fraudDetectionService = new FraudDetectionService(List.of(
                new AmountThresholdRule(properties),
                new SuspiciousAccountRule(properties),
                new SuspiciousMerchantRule(properties)
        ));
    }

    @Test
    void shouldFlagAmountAboveThreshold() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-normal", "merchant-ok", "1500"));

        assertThat(decision.fraudulent()).isTrue();
        assertThat(decision.reasons()).contains("Amount exceeded threshold");
    }

    @Test
    void shouldFlagSuspiciousAccount() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-watch", "merchant-ok", "20"));

        assertThat(decision.fraudulent()).isTrue();
        assertThat(decision.reasons()).contains("Account is on the suspicious watchlist");
    }

    @Test
    void shouldFlagSuspiciousMerchant() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-ok", "merchant-risk", "20"));

        assertThat(decision.fraudulent()).isTrue();
        assertThat(decision.reasons()).contains("Merchant is on the suspicious watchlist");
    }

    @Test
    void shouldApproveLegitimateTransaction() {
        FraudDecision decision = fraudDetectionService.evaluate(transaction("acct-ok", "merchant-ok", "20"));

        assertThat(decision.fraudulent()).isFalse();
        assertThat(decision.reasons()).isEmpty();
    }

    private TransactionEvent transaction(String accountId, String merchantId, String amount) {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");
        return new TransactionEvent(
                "tx-1",
                accountId,
                merchantId,
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal(amount),
                now,
                now
        );
    }
}
