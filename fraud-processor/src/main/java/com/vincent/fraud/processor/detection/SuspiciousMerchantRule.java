package com.vincent.fraud.processor.detection;

import com.vincent.fraud.processor.config.FraudRulesProperties;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousMerchantRule implements FraudRule {

    private final FraudRulesProperties properties;

    public SuspiciousMerchantRule(FraudRulesProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> evaluate(TransactionEvent transactionEvent) {
        if (properties.suspiciousMerchants().contains(transactionEvent.merchantId())) {
            return Optional.of("Merchant is on the suspicious watchlist");
        }
        return Optional.empty();
    }
}
