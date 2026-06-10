package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vincent.fraud.shared.model.AlertEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MnsAlertPublisherTest {

    @Test
    void shouldPublishAlertMessage() {
        RecordingMnsAlertPublisher publisher = new RecordingMnsAlertPublisher();

        String messageId = publisher.publish(alert());

        assertThat(messageId).isEqualTo("msg-1");
        assertThat(publisher.publishedMessage.getMessageBodyAsRawString()).contains("\"alertId\":\"alert-1\"");
    }

    @Test
    void shouldSerializeAlertEvent() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        MnsAlertPublisher publisher = new MnsAlertPublisher(null, objectMapper);

        String payload = ReflectionTestUtils.invokeMethod(publisher, "serialize", alert());

        assertThat(payload).contains("\"severity\":\"HIGH\"");
        assertThat(payload).contains("\"accountId\":\"acct-1\"");
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
        MnsAlertPublisher publisher = new MnsAlertPublisher(null, failingObjectMapper);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(publisher, "serialize", alert()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize alert event");
    }

    private AlertEvent alert() {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        return new AlertEvent(
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
                now,
                now
        );
    }

    private static final class RecordingMnsAlertPublisher extends MnsAlertPublisher {

        private Message publishedMessage;

        private RecordingMnsAlertPublisher() {
            super(null, JsonMapper.builder().addModule(new JavaTimeModule()).build());
        }

        @Override
        Message putMessage(Message message) {
            this.publishedMessage = message;
            Message result = new Message();
            result.setMessageId("msg-1");
            return result;
        }
    }
}
