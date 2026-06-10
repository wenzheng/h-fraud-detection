package com.vincent.fraud.processor.detection;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.processor.config.FraudRulesProperties;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleTests {

    private final FraudRulesProperties properties = new FraudRulesProperties(
            new BigDecimal("100.00"),
            List.of("acct-watch"),
            List.of("merchant-watch")
    );

    @Test
    void amountThresholdRuleShouldMatchLargeTransaction() {
        AmountThresholdRule rule = new AmountThresholdRule(properties);

        assertThat(rule.evaluate(transaction("acct-1", "merchant-1", new BigDecimal("101.00"))))
                .contains("Amount exceeded threshold");
    }

    @Test
    void suspiciousAccountRuleShouldMatchWatchlistAccount() {
        SuspiciousAccountRule rule = new SuspiciousAccountRule(properties);

        assertThat(rule.evaluate(transaction("acct-watch", "merchant-1", new BigDecimal("10.00"))))
                .contains("Account is on the suspicious watchlist");
    }

    @Test
    void suspiciousMerchantRuleShouldMatchWatchlistMerchant() {
        SuspiciousMerchantRule rule = new SuspiciousMerchantRule(properties);

        assertThat(rule.evaluate(transaction("acct-1", "merchant-watch", new BigDecimal("10.00"))))
                .contains("Merchant is on the suspicious watchlist");
    }

    private TransactionEvent transaction(String accountId, String merchantId, BigDecimal amount) {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        return new TransactionEvent(
                "tx-1",
                accountId,
                merchantId,
                "device-1",
                "10.0.0.1",
                "USD",
                amount,
                now,
                now
        );
    }
}
