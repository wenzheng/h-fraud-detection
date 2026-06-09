package com.vincent.fraud.detection;

import com.vincent.fraud.config.FraudProperties;
import com.vincent.fraud.model.Transaction;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousAccountRule implements FraudRule {

    private final FraudProperties properties;

    public SuspiciousAccountRule(FraudProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> evaluate(Transaction transaction) {
        if (properties.suspiciousAccounts().contains(transaction.accountId())) {
            return Optional.of("Account is on the suspicious watchlist");
        }
        return Optional.empty();
    }
}
