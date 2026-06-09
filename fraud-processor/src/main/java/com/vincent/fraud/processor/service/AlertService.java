package com.vincent.fraud.processor.service;

import com.vincent.fraud.shared.model.AlertEvent;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final List<AlertNotifier> alertNotifiers;

    public AlertService(List<AlertNotifier> alertNotifiers) {
        this.alertNotifiers = alertNotifiers;
    }

    public void raise(TransactionEvent transactionEvent, List<String> reasons) {
        AlertEvent.Severity severity = reasons.size() > 1 ? AlertEvent.Severity.HIGH : AlertEvent.Severity.MEDIUM;
        AlertEvent alertEvent = new AlertEvent(
                UUID.randomUUID().toString(),
                transactionEvent.transactionId(),
                transactionEvent.accountId(),
                reasons,
                severity,
                Instant.now()
        );
        log.warn("fraud-alert transactionId={} accountId={} severity={} reasons={}",
                transactionEvent.transactionId(), transactionEvent.accountId(), severity, reasons);
        alertNotifiers.forEach(notifier -> notifier.notify(alertEvent, transactionEvent));
    }
}
