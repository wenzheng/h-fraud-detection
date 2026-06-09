package com.vincent.fraud.processor.detection;

import com.vincent.fraud.shared.model.FraudDecision;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FraudDetectionService {

    private final List<FraudRule> fraudRules;

    public FraudDetectionService(List<FraudRule> fraudRules) {
        this.fraudRules = fraudRules;
    }

    public FraudDecision evaluate(TransactionEvent transactionEvent) {
        List<String> reasons = fraudRules.stream()
                .map(rule -> rule.evaluate(transactionEvent))
                .flatMap(Optional::stream)
                .toList();
        if (reasons.isEmpty()) {
            return FraudDecision.approved();
        }
        return FraudDecision.flagged(reasons);
    }
}
