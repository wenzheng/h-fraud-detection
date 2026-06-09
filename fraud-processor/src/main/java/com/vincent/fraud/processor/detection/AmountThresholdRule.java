package com.vincent.fraud.processor.detection;

import com.vincent.fraud.processor.config.FraudRulesProperties;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AmountThresholdRule implements FraudRule {

    private final FraudRulesProperties properties;

    public AmountThresholdRule(FraudRulesProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> evaluate(TransactionEvent transactionEvent) {
        if (transactionEvent.amount().compareTo(properties.amountThreshold()) > 0) {
            return Optional.of("Amount exceeded threshold");
        }
        return Optional.empty();
    }
}
