package com.vincent.fraud.processor.service;

import com.vincent.fraud.shared.model.AlertEvent;
import com.vincent.fraud.shared.model.TransactionEvent;

public interface AlertNotifier {

    void notify(AlertEvent alertEvent, TransactionEvent transactionEvent);
}
