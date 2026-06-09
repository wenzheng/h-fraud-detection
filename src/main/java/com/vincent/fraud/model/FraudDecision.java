package com.vincent.fraud.model;

import java.util.List;

public record FraudDecision(
        boolean fraudulent,
        List<String> reasons
) {
    public static FraudDecision clear() {
        return new FraudDecision(false, List.of());
    }

    public static FraudDecision flagged(List<String> reasons) {
        return new FraudDecision(true, reasons);
    }
}
