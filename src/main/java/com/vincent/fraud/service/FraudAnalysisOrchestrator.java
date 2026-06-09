package com.vincent.fraud.service;

import com.vincent.fraud.detection.FraudDetectionService;
import com.vincent.fraud.model.FraudDecision;
import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class FraudAnalysisOrchestrator {

    private final FraudDetectionService fraudDetectionService;
    private final TransactionRepository transactionRepository;
    private final AlertService alertService;

    public FraudAnalysisOrchestrator(
            FraudDetectionService fraudDetectionService,
            TransactionRepository transactionRepository,
            AlertService alertService
    ) {
        this.fraudDetectionService = fraudDetectionService;
        this.transactionRepository = transactionRepository;
        this.alertService = alertService;
    }

    public void analyze(Transaction transaction) {
        FraudDecision decision = fraudDetectionService.evaluate(transaction);
        Transaction updated = transaction.withStatus(decision.fraudulent() ? Transaction.Status.FLAGGED : Transaction.Status.APPROVED);
        transactionRepository.save(updated);

        if (decision.fraudulent()) {
            alertService.raise(updated, decision.reasons());
        }
    }
}
