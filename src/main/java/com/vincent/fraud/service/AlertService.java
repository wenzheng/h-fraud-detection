package com.vincent.fraud.service;

import com.vincent.fraud.model.Alert;
import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.repository.AlertRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final List<AlertNotifier> alertNotifiers;

    public AlertService(AlertRepository alertRepository, List<AlertNotifier> alertNotifiers) {
        this.alertRepository = alertRepository;
        this.alertNotifiers = alertNotifiers;
    }

    public void raise(Transaction transaction, List<String> reasons) {
        Alert.Severity severity = reasons.size() > 1 ? Alert.Severity.HIGH : Alert.Severity.MEDIUM;
        Alert alert = new Alert(
                UUID.randomUUID().toString(),
                transaction.id(),
                transaction.accountId(),
                reasons,
                severity,
                Instant.now()
        );
        alertRepository.save(alert);
        log.warn("fraud-alert transactionId={} accountId={} severity={} reasons={}",
                transaction.id(), transaction.accountId(), severity, reasons);
        alertNotifiers.forEach(notifier -> notifier.notify(alert, transaction));
    }
}
