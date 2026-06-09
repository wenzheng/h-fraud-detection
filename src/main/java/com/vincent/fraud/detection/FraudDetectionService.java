package com.vincent.fraud.detection;

import com.vincent.fraud.model.FraudDecision;
import com.vincent.fraud.model.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FraudDetectionService {

    private final List<FraudRule> fraudRules;

    public FraudDetectionService(List<FraudRule> fraudRules) {
        this.fraudRules = fraudRules;
    }

    public FraudDecision evaluate(Transaction transaction) {
        List<String> reasons = fraudRules.stream()
                .map(rule -> rule.evaluate(transaction))
                .flatMap(Optional::stream)
                .toList();

        if (reasons.isEmpty()) {
            return FraudDecision.clear();
        }
        return FraudDecision.flagged(reasons);
    }
}
