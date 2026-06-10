package com.vincent.fraud.alert.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.shared.model.AlertEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TelegramAlertSenderTest {

    @Test
    void shouldReportTelegramChannelAndAcceptSend() {
        TelegramAlertSender sender = new TelegramAlertSender();

        assertThat(sender.channel()).isEqualTo(AlertRoutingProperties.Channel.TELEGRAM);
        sender.send(new AlertEvent(
                "alert-1",
                "tx-1",
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                new BigDecimal("42.00"),
                "USD",
                List.of("Amount exceeded threshold"),
                AlertEvent.Severity.HIGH,
                Instant.parse("2026-06-11T01:00:00Z"),
                Instant.parse("2026-06-11T01:00:01Z")
        ));
    }
}
