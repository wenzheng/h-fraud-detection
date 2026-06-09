package com.vincent.fraud.detection;

import com.vincent.fraud.config.FraudProperties;
import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.repository.TransactionRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class VelocityRule implements FraudRule {

    private final FraudProperties properties;
    private final TransactionRepository transactionRepository;

    public VelocityRule(FraudProperties properties, TransactionRepository transactionRepository) {
        this.properties = properties;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Optional<String> evaluate(Transaction transaction) {
        Instant windowStart = transaction.occurredAt().minus(properties.velocityWindow());
        long recentCount = transactionRepository.findByAccountWithin(transaction.accountId(), windowStart).size();
        if (recentCount >= properties.velocityCountThreshold()) {
            return Optional.of("Velocity rule triggered for account");
        }
        return Optional.empty();
    }
}
