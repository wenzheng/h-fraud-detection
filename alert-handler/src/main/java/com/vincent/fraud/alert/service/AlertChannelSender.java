package com.vincent.fraud.alert.service;

import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.shared.model.AlertEvent;

public interface AlertChannelSender {

    AlertRoutingProperties.Channel channel();

    void send(AlertEvent alertEvent);
}
