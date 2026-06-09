package com.vincent.fraud.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vincent.fraud.config.TelegramProperties;
import com.vincent.fraud.model.Alert;
import com.vincent.fraud.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramAlertNotifierTest {

    @Test
    void shouldSendTelegramMessageWhenEnabledAndConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = new TelegramProperties(
                true,
                "test-token",
                "chat-123",
                "https://api.telegram.org",
                true
        );
        TelegramAlertNotifier notifier = new TelegramAlertNotifier(properties, builder);

        server.expect(requestTo("/bottest-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "chat_id": "chat-123",
                          "parse_mode": "HTML",
                          "disable_notification": true
                        }
                        """, false))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{}}", MediaType.APPLICATION_JSON));

        notifier.notify(alert(), transaction());

        server.verify();
    }

    @Test
    void shouldSkipTelegramCallWhenDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = new TelegramProperties(
                false,
                "test-token",
                "chat-123",
                "https://api.telegram.org",
                false
        );
        TelegramAlertNotifier notifier = new TelegramAlertNotifier(properties, builder);

        notifier.notify(alert(), transaction());

        server.verify();
    }

    private Alert alert() {
        return new Alert(
                "alert-1",
                "tx-1",
                "acct-test",
                List.of("Amount exceeded threshold"),
                Alert.Severity.MEDIUM,
                Instant.parse("2026-06-09T12:00:00Z")
        );
    }

    private Transaction transaction() {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");
        return new Transaction(
                "tx-1",
                "acct-test",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("100.00"),
                now,
                now,
                Transaction.Status.FLAGGED
        );
    }
}
