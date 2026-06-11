package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.processor.detection.FraudDetectionService;
import com.vincent.fraud.shared.model.FraudDecision;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class FraudProcessingServiceTest {

    @Test
    void shouldRaiseAlertWhenTransactionIsFraudulent() {
        RecordingAlertService alertService = new RecordingAlertService();
        RecordingProcessingMetricsService metricsService = new RecordingProcessingMetricsService();
        FraudProcessingService service = new FraudProcessingService(
                new StubFraudDetectionService(FraudDecision.flagged(List.of("Amount exceeded threshold"))),
                alertService,
                metricsService
        );
        TransactionEvent transaction = TestTransactions.transaction();

        service.process(transaction);

        assertThat(alertService.transaction).isEqualTo(transaction);
        assertThat(alertService.reasons).containsExactly("Amount exceeded threshold");
        assertThat(metricsService.incrementCount).isEqualTo(1);
    }

    @Test
    void shouldNotRaiseAlertWhenTransactionIsApproved() {
        RecordingAlertService alertService = new RecordingAlertService();
        RecordingProcessingMetricsService metricsService = new RecordingProcessingMetricsService();
        FraudProcessingService service = new FraudProcessingService(
                new StubFraudDetectionService(FraudDecision.approved()),
                alertService,
                metricsService
        );

        service.process(TestTransactions.transaction());

        assertThat(alertService.transaction).isNull();
        assertThat(alertService.reasons).isNull();
        assertThat(metricsService.incrementCount).isEqualTo(1);
    }

    private static final class StubFraudDetectionService extends FraudDetectionService {

        private final FraudDecision decision;

        private StubFraudDetectionService(FraudDecision decision) {
            super(List.of());
            this.decision = decision;
        }

        @Override
        public FraudDecision evaluate(TransactionEvent transactionEvent) {
            return decision;
        }
    }

    private static final class RecordingAlertService extends AlertService {

        private TransactionEvent transaction;
        private List<String> reasons;

        private RecordingAlertService() {
            super(alertEvent -> "unused", new AlertSeverityResolver());
        }

        @Override
        public void raise(TransactionEvent transactionEvent, List<String> reasons) {
            this.transaction = transactionEvent;
            this.reasons = reasons;
        }
    }

    private static final class RecordingProcessingMetricsService extends ProcessingMetricsService {

        private int incrementCount;

        private RecordingProcessingMetricsService() {
            super(new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                    "fraud-processor",
                    "pod-1",
                    "node-1");
        }

        @Override
        public void incrementProcessedCount() {
            incrementCount++;
        }
    }
}
