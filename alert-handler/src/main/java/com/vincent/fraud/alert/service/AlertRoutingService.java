package com.vincent.fraud.alert.service;

import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.shared.model.AlertEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertRoutingService {

    private static final Logger log = LoggerFactory.getLogger(AlertRoutingService.class);

    private final AlertRoutingProperties properties;
    private final Map<AlertRoutingProperties.Channel, AlertChannelSender> senders;

    public AlertRoutingService(AlertRoutingProperties properties, List<AlertChannelSender> senderList) {
        this.properties = properties;
        this.senders = new EnumMap<>(AlertRoutingProperties.Channel.class);
        senderList.forEach(sender -> senders.put(sender.channel(), sender));
    }

    public void route(AlertEvent alertEvent) {
        log.warn("fraud-alert-received alertId={} transactionId={} severity={} reasons={}",
                alertEvent.alertId(), alertEvent.transactionId(), alertEvent.severity(), alertEvent.reasons());
        for (AlertRoutingProperties.Channel channel : actionsFor(alertEvent.severity())) {
            if (channel == AlertRoutingProperties.Channel.LOG) {
                log.info("fraud-alert-logged alertId={} severity={} accountId={}",
                        alertEvent.alertId(), alertEvent.severity(), alertEvent.accountId());
                continue;
            }
            AlertChannelSender sender = senders.get(channel);
            if (sender == null) {
                log.warn("No alert sender configured for channel={} alertId={}", channel, alertEvent.alertId());
                continue;
            }
            sender.send(alertEvent);
        }
    }

    private List<AlertRoutingProperties.Channel> actionsFor(AlertEvent.Severity severity) {
        return switch (severity) {
            case HIGH -> properties.highActions();
            case MEDIUM -> properties.mediumActions();
            case LOW -> properties.lowActions();
        };
    }
}
