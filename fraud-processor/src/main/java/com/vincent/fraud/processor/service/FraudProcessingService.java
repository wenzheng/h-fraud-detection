package com.vincent.fraud.processor.service;

import com.vincent.fraud.processor.detection.FraudDetectionService;
import com.vincent.fraud.shared.model.FraudDecision;
import com.vincent.fraud.shared.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FraudProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FraudProcessingService.class);

    private final FraudDetectionService fraudDetectionService;
    private final AlertService alertService;
    private final ProcessingMetricsService processingMetricsService;

    public FraudProcessingService(
            FraudDetectionService fraudDetectionService,
            AlertService alertService,
            ProcessingMetricsService processingMetricsService
    ) {
        this.fraudDetectionService = fraudDetectionService;
        this.alertService = alertService;
        this.processingMetricsService = processingMetricsService;
    }

    public void process(TransactionEvent transactionEvent) {
        FraudDecision decision = fraudDetectionService.evaluate(transactionEvent);
        if (decision.fraudulent()) {
            alertService.raise(transactionEvent, decision.reasons());
        } else {
            log.info("transaction-approved transactionId={} accountId={}",
                    transactionEvent.transactionId(), transactionEvent.accountId());
        }
        processingMetricsService.incrementProcessedCount();
    }
}
