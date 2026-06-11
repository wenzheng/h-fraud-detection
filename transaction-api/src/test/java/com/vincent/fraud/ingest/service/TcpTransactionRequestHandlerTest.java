package com.vincent.fraud.ingest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TcpTransactionRequestHandlerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldPublishValidTcpPayload() {
        AtomicReference<PublishTransactionCommand> captured = new AtomicReference<>();
        TransactionPublisherService publisherService = transactionPublisherService(command -> {
            captured.set(command);
            return new PublishedTransaction("tx-1", "msg-1", Instant.parse("2026-06-11T01:00:01Z"));
        });
        TcpTransactionRequestHandler handler = new TcpTransactionRequestHandler(
                objectMapper,
                validator,
                publisherService
        );

        String response = handler.handle("""
                {"accountId":"acct-1","merchantId":"merchant-1","deviceId":"device-1","ipAddress":"10.0.0.1","currency":"USD","amount":42.00,"occurredAt":"2026-06-11T01:00:00Z"}
                """.trim());

        assertThat(response).contains("\"status\":\"QUEUED\"");
        assertThat(response).contains("\"transactionId\":\"tx-1\"");
        assertThat(captured.get()).isEqualTo(new PublishTransactionCommand(
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("42.00"),
                Instant.parse("2026-06-11T01:00:00Z")
        ));
    }

    @Test
    void shouldReturnValidationErrorForInvalidPayload() {
        TcpTransactionRequestHandler handler = new TcpTransactionRequestHandler(
                objectMapper,
                validator,
                transactionPublisherService(command -> {
                    throw new IllegalStateException("should not be called");
                })
        );

        String response = handler.handle("""
                {"accountId":"","merchantId":"merchant-1","deviceId":"device-1","ipAddress":"10.0.0.1","currency":"USD","amount":0}
                """.trim());

        assertThat(response).contains("\"error\":\"VALIDATION_ERROR\"");
        assertThat(response).contains("accountId");
    }

    @Test
    void shouldReturnInvalidJsonErrorForMalformedPayload() {
        TcpTransactionRequestHandler handler = new TcpTransactionRequestHandler(
                objectMapper,
                validator,
                transactionPublisherService(command -> {
                    throw new IllegalStateException("should not be called");
                })
        );

        String response = handler.handle("dGVzdA");

        assertThat(response).contains("\"error\":\"INVALID_JSON\"");
    }

    @Test
    void shouldReturnPublishErrorWhenPublisherFails() {
        TcpTransactionRequestHandler handler = new TcpTransactionRequestHandler(
                objectMapper,
                validator,
                transactionPublisherService(command -> {
                    throw new IllegalStateException("publish failed");
                })
        );

        String response = handler.handle("""
                {"accountId":"acct-1","merchantId":"merchant-1","deviceId":"device-1","ipAddress":"10.0.0.1","currency":"USD","amount":42.00}
                """.trim());

        assertThat(response).contains("\"error\":\"PUBLISH_ERROR\"");
        assertThat(response).contains("publish failed");
    }

    @Test
    void shouldThrowWhenResponseSerializationFails() {
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public <T> T readValue(String content, Class<T> valueType) {
                return valueType.cast(new com.vincent.fraud.ingest.controller.TransactionRequest(
                        "acct-1",
                        "merchant-1",
                        "device-1",
                        "10.0.0.1",
                        "USD",
                        new BigDecimal("42.00"),
                        Instant.parse("2026-06-11T01:00:00Z")
                ));
            }

            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("bad response json") {
                };
            }
        };
        TcpTransactionRequestHandler handler = new TcpTransactionRequestHandler(
                failingObjectMapper,
                validator,
                transactionPublisherService(command ->
                        new PublishedTransaction("tx-1", "msg-1", Instant.parse("2026-06-11T01:00:01Z")))
        );

        assertThatThrownBy(() -> handler.handle("{\"ignored\":true}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize TCP response");
    }

    private TransactionPublisherService transactionPublisherService(TransactionPublisher transactionPublisher) {
        return new TransactionPublisherService(
                transactionPublisher,
                new IngressMetricsService(
                        new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                        "transaction-api",
                        "pod-1",
                        "node-1"
                )
        );
    }
}
