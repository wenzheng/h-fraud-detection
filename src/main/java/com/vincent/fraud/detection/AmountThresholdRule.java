package com.vincent.fraud.detection;

import com.vincent.fraud.config.FraudProperties;
import com.vincent.fraud.model.Transaction;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AmountThresholdRule implements FraudRule {

    private final FraudProperties properties;

    public AmountThresholdRule(FraudProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> evaluate(Transaction transaction) {
        if (transaction.amount().compareTo(properties.amountThreshold()) > 0) {
            return Optional.of("Amount exceeded threshold");
        }
        return Optional.empty();
    }
}
