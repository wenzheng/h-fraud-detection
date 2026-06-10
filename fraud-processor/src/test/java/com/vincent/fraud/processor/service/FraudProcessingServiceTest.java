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
        FraudProcessingService service = new FraudProcessingService(
                new StubFraudDetectionService(FraudDecision.flagged(List.of("Amount exceeded threshold"))),
                alertService
        );
        TransactionEvent transaction = TestTransactions.transaction();

        service.process(transaction);

        assertThat(alertService.transaction).isEqualTo(transaction);
        assertThat(alertService.reasons).containsExactly("Amount exceeded threshold");
    }

    @Test
    void shouldNotRaiseAlertWhenTransactionIsApproved() {
        RecordingAlertService alertService = new RecordingAlertService();
        FraudProcessingService service = new FraudProcessingService(
                new StubFraudDetectionService(FraudDecision.approved()),
                alertService
        );

        service.process(TestTransactions.transaction());

        assertThat(alertService.transaction).isNull();
        assertThat(alertService.reasons).isNull();
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
}
