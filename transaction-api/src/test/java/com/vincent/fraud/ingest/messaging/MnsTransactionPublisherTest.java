package com.vincent.fraud.ingest.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.model.Message;
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

    @Test
    void shouldPublishTransactionEventToQueue() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        Message result = new Message();
        result.setMessageId("msg-1");
        TestableMnsTransactionPublisher publisher = new TestableMnsTransactionPublisher(objectMapper, result);

        var published = publisher.publish(command());

        assertThat(publisher.capturedMessage.getMessageBodyAsRawString()).contains("\"accountId\":\"acct-1\"");
        assertThat(published.queueMessageId()).isEqualTo("msg-1");
        assertThat(published.transactionId()).isNotBlank();
        assertThat(published.queuedAt()).isNotNull();
    }

    @Test
    void shouldWrapQueuePublishFailure() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        TestableMnsTransactionPublisher publisher = new TestableMnsTransactionPublisher(objectMapper, null);
        publisher.clientExceptionToThrow = new ClientException("boom", "req-1");

        assertThatThrownBy(() -> publisher.publish(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to publish transaction to Alibaba Cloud SMQ");
    }

    private com.vincent.fraud.shared.model.TransactionEvent event() {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        PublishTransactionCommand command = command();
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

    private PublishTransactionCommand command() {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        return new PublishTransactionCommand(
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("42.00"),
                now
        );
    }

    private static final class TestableMnsTransactionPublisher extends MnsTransactionPublisher {

        private Message capturedMessage;
        private Message messageToReturn;
        private ClientException clientExceptionToThrow;

        private TestableMnsTransactionPublisher(ObjectMapper objectMapper, Message messageToReturn) {
            super(null, objectMapper);
            this.messageToReturn = messageToReturn;
        }

        @Override
        Message putMessage(Message message) {
            capturedMessage = message;
            if (clientExceptionToThrow != null) {
                throw clientExceptionToThrow;
            }
            return messageToReturn;
        }
    }
}
