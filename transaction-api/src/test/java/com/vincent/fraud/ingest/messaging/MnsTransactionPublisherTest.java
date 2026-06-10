package com.vincent.fraud.ingest.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vincent.fraud.ingest.service.PublishTransactionCommand;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MnsTransactionPublisherTest {

    @Test
    void shouldSerializeTransactionEvent() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        MnsTransactionPublisher publisher = new MnsTransactionPublisher(null, objectMapper);

        String payload = ReflectionTestUtils.invokeMethod(publisher, "serialize", event());

        assertThat(payload).contains("\"accountId\":\"acct-1\"");
        assertThat(payload).contains("\"currency\":\"USD\"");
    }

    @Test
    void shouldWrapSerializationFailure() {
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("bad json") {
                };
            }
        };
        MnsTransactionPublisher publisher = new MnsTransactionPublisher(null, failingObjectMapper);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(publisher, "serialize", event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize transaction event");
    }

    private com.vincent.fraud.shared.model.TransactionEvent event() {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        PublishTransactionCommand command = new PublishTransactionCommand(
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("42.00"),
                now
        );
        return new com.vincent.fraud.shared.model.TransactionEvent(
                "tx-1",
                command.accountId(),
                command.merchantId(),
                command.deviceId(),
                command.ipAddress(),
                command.currency(),
                command.amount(),
                command.occurredAt(),
                now
        );
    }
}
