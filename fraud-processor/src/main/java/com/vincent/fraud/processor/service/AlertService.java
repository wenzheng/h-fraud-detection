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

    private final AlertPublisher alertPublisher;
    private final AlertSeverityResolver alertSeverityResolver;

    public AlertService(AlertPublisher alertPublisher, AlertSeverityResolver alertSeverityResolver) {
        this.alertPublisher = alertPublisher;
        this.alertSeverityResolver = alertSeverityResolver;
    }

    public void raise(TransactionEvent transactionEvent, List<String> reasons) {
        AlertEvent.Severity severity = alertSeverityResolver.resolve(reasons);
        AlertEvent alertEvent = new AlertEvent(
                UUID.randomUUID().toString(),
                transactionEvent.transactionId(),
                transactionEvent.accountId(),
                transactionEvent.merchantId(),
                transactionEvent.deviceId(),
                transactionEvent.ipAddress(),
                transactionEvent.amount(),
                transactionEvent.currency(),
                reasons,
                severity,
                transactionEvent.occurredAt(),
                Instant.now()
        );
        String messageId = alertPublisher.publish(alertEvent);
        log.warn("fraud-alert transactionId={} accountId={} severity={} reasons={}",
                transactionEvent.transactionId(), transactionEvent.accountId(), severity, reasons);
        log.info("fraud-alert-enqueued alertId={} transactionId={} queueMessageId={}",
                alertEvent.alertId(), transactionEvent.transactionId(), messageId);
    }
}
