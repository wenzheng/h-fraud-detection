package com.vincent.fraud.processor.detection;

import com.vincent.fraud.shared.model.TransactionEvent;
import java.util.Optional;

public interface FraudRule {

    Optional<String> evaluate(TransactionEvent transactionEvent);
}
