package com.vincent.fraud.processor.service;

import com.vincent.fraud.shared.model.AlertEvent;

public interface AlertPublisher {

    String publish(AlertEvent alertEvent);
}
