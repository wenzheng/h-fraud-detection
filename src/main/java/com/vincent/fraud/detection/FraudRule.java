package com.vincent.fraud.detection;

import com.vincent.fraud.model.Transaction;
import java.util.Optional;

public interface FraudRule {

    Optional<String> evaluate(Transaction transaction);
}
