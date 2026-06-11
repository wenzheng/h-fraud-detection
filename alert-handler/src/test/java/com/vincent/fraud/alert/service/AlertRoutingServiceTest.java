package com.vincent.fraud.alert.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.shared.model.AlertEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertRoutingServiceTest {

    @Test
    void shouldRouteHighSeverityToTelegramAndLog() {
        List<AlertEvent> telegramAlerts = new ArrayList<>();
        List<AlertEvent> emailAlerts = new ArrayList<>();
        RecordingAlertMetricsService metricsService = new RecordingAlertMetricsService();
        AlertRoutingService service = new AlertRoutingService(
                properties(),
                List.of(
                        sender(AlertRoutingProperties.Channel.TELEGRAM, telegramAlerts),
                        sender(AlertRoutingProperties.Channel.EMAIL, emailAlerts)
                ),
                metricsService
        );

        service.route(alert(AlertEvent.Severity.HIGH));

        assertThat(telegramAlerts).hasSize(1);
        assertThat(emailAlerts).isEmpty();
        assertThat(metricsService.incrementCount).isEqualTo(1);
    }

    @Test
    void shouldRouteMediumSeverityToEmailAndLog() {
        List<AlertEvent> telegramAlerts = new ArrayList<>();
        List<AlertEvent> emailAlerts = new ArrayList<>();
        RecordingAlertMetricsService metricsService = new RecordingAlertMetricsService();
        AlertRoutingService service = new AlertRoutingService(
                properties(),
                List.of(
                        sender(AlertRoutingProperties.Channel.TELEGRAM, telegramAlerts),
                        sender(AlertRoutingProperties.Channel.EMAIL, emailAlerts)
                ),
                metricsService
        );

        service.route(alert(AlertEvent.Severity.MEDIUM));

        assertThat(telegramAlerts).isEmpty();
        assertThat(emailAlerts).hasSize(1);
        assertThat(metricsService.incrementCount).isEqualTo(1);
    }

    @Test
    void shouldOnlyLogLowSeverityAlerts() {
        List<AlertEvent> telegramAlerts = new ArrayList<>();
        List<AlertEvent> emailAlerts = new ArrayList<>();
        RecordingAlertMetricsService metricsService = new RecordingAlertMetricsService();
        AlertRoutingService service = new AlertRoutingService(
                properties(),
                List.of(
                        sender(AlertRoutingProperties.Channel.TELEGRAM, telegramAlerts),
                        sender(AlertRoutingProperties.Channel.EMAIL, emailAlerts)
                ),
                metricsService
        );

        service.route(alert(AlertEvent.Severity.LOW));

        assertThat(telegramAlerts).isEmpty();
        assertThat(emailAlerts).isEmpty();
        assertThat(metricsService.incrementCount).isEqualTo(1);
    }

    private AlertRoutingProperties properties() {
        return new AlertRoutingProperties(
                List.of(AlertRoutingProperties.Channel.TELEGRAM, AlertRoutingProperties.Channel.LOG),
                List.of(AlertRoutingProperties.Channel.EMAIL, AlertRoutingProperties.Channel.LOG),
                List.of(AlertRoutingProperties.Channel.LOG)
        );
    }

    private AlertChannelSender sender(AlertRoutingProperties.Channel channel, List<AlertEvent> sink) {
        return new AlertChannelSender() {
            @Override
            public AlertRoutingProperties.Channel channel() {
                return channel;
            }

            @Override
            public void send(AlertEvent alertEvent) {
                sink.add(alertEvent);
            }
        };
    }

    private AlertEvent alert(AlertEvent.Severity severity) {
        Instant now = Instant.parse("2026-06-10T00:00:00Z");
        return new AlertEvent(
                "alert-1",
                "tx-1",
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                new BigDecimal("42.00"),
                "USD",
                List.of("reason"),
                severity,
                now,
                now
        );
    }

    private static final class RecordingAlertMetricsService extends AlertMetricsService {

        private int incrementCount;

        private RecordingAlertMetricsService() {
            super(new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                    "alert-handler",
                    "pod-1",
                    "node-1");
        }

        @Override
        public void incrementHandledCount() {
            incrementCount++;
        }
    }
}
