package com.vincent.fraud.alert.service;

import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.shared.model.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramAlertSender implements AlertChannelSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertSender.class);

    @Override
    public AlertRoutingProperties.Channel channel() {
        return AlertRoutingProperties.Channel.TELEGRAM;
    }

    @Override
    public void send(AlertEvent alertEvent) {
        log.info("telegram-alert-placeholder alertId={} severity={} accountId={}",
                alertEvent.alertId(), alertEvent.severity(), alertEvent.accountId());
    }
}
