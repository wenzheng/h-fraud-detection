package com.vincent.fraud.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vincent.fraud.shared.model.AlertEvent;
import java.math.BigDecimal;
import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.alert.config.ConsumerProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MnsAlertConsumerLifecycleTest {

    @Test
    void shouldSubmitConfiguredNumberOfPollersOnStart() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        MnsAlertConsumerLifecycle lifecycle = new MnsAlertConsumerLifecycle(
                null,
                new ConsumerProperties(3, 1, 1),
                executorService,
                new ObjectMapper(),
                new AlertRoutingService(
                        new AlertRoutingProperties(null, null, null),
                        List.of(new TelegramAlertSender(), new EmailAlertSender()),
                        new AlertMetricsService(
                                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                                "alert-handler",
                                "pod-1",
                                "node-1"
                        )
                )
        );

        lifecycle.start();

        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(executorService.submittedTasks).hasSize(3);
    }

    @Test
    void shouldStopLifecycle() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        MnsAlertConsumerLifecycle lifecycle = new MnsAlertConsumerLifecycle(
                null,
                new ConsumerProperties(1, 1, 1),
                executorService,
                new ObjectMapper(),
                new AlertRoutingService(
                        new AlertRoutingProperties(null, null, null),
                        List.of(new TelegramAlertSender(), new EmailAlertSender()),
                        new AlertMetricsService(
                                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                                "alert-handler",
                                "pod-1",
                                "node-1"
                        )
                )
        );
        lifecycle.start();

        lifecycle.stop();

        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldProcessPoppedMessagesInPollLoop() throws Exception {
        RecordingAlertRoutingService routingService = new RecordingAlertRoutingService();
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 2, 5),
                objectMapper(),
                routingService
        );
        Message message = message("msg-1", "rh-1", objectMapper().writeValueAsString(alert()));
        lifecycle.messagesToPop = List.of(message);
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(routingService.routedAlerts).hasSize(1);
        assertThat(routingService.routedAlerts.get(0).alertId()).isEqualTo("alert-1");
        assertThat(lifecycle.acknowledgedReceiptHandles).containsExactly("rh-1");
        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
    }

    @Test
    void shouldAcknowledgeMessageWhenProcessSucceeds() throws Exception {
        RecordingAlertRoutingService routingService = new RecordingAlertRoutingService();
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                routingService
        );
        Message message = message("msg-1", "rh-1", objectMapper().writeValueAsString(alert()));

        lifecycle.processMessage(message);

        assertThat(routingService.routedAlerts).hasSize(1);
        assertThat(lifecycle.acknowledgedReceiptHandles).containsExactly("rh-1");
    }

    @Test
    void shouldNotAcknowledgeMessageWhenProcessFails() {
        RecordingAlertRoutingService routingService = new RecordingAlertRoutingService();
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public <T> T readValue(String content, Class<T> valueType) {
                throw new IllegalStateException("boom");
            }
        };
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                failingObjectMapper,
                routingService
        );
        Message message = message("msg-1", "rh-1", "{bad json");

        lifecycle.processMessage(message);

        assertThat(routingService.routedAlerts).isEmpty();
        assertThat(lifecycle.acknowledgedReceiptHandles).isEmpty();
    }

    @Test
    void shouldAcknowledgeMessageWhenPayloadIsMalformedJson() {
        RecordingAlertRoutingService routingService = new RecordingAlertRoutingService();
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException {
                throw new JsonParseException(null, "bad json");
            }
        };
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                failingObjectMapper,
                routingService
        );
        Message message = message("msg-1", "rh-1", "dGVzdA");

        lifecycle.processMessage(message);

        assertThat(routingService.routedAlerts).isEmpty();
        assertThat(lifecycle.acknowledgedReceiptHandles).containsExactly("rh-1");
    }

    @Test
    void shouldTreatMessageNotExistAsEmptyQueue() {
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingAlertRoutingService()
        );

        assertThat(lifecycle.isQueueEmpty(new ClientException(
                "MessageNotExist",
                "Message not exist",
                "req-1",
                null
        ))).isTrue();
        assertThat(lifecycle.isQueueEmpty(new ClientException("MessageNotExist appeared in parser output", "req-2")))
                .isTrue();
        assertThat(lifecycle.isQueueEmpty(new ClientException("boom", "OtherError"))).isFalse();
    }

    @Test
    void shouldContinueWhenNoMessagesAreReturned() {
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingAlertRoutingService()
        );
        lifecycle.messagesToPop = List.of();
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
        assertThat(lifecycle.acknowledgedReceiptHandles).isEmpty();
    }

    @Test
    void shouldContinueWhenQueueReportsNoMessagesAvailable() {
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingAlertRoutingService()
        );
        lifecycle.clientExceptionToThrow = new ClientException(
                MESSAGE_NOT_EXIST_ERROR_CODE,
                "Message not exist.",
                "req-1",
                null
        );
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
        assertThat(lifecycle.acknowledgedReceiptHandles).isEmpty();
    }

    @Test
    void shouldHandleUnexpectedClientExceptionInPollLoop() {
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingAlertRoutingService()
        );
        lifecycle.clientExceptionToThrow = new ClientException("boom", "req-1");
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
    }

    @Test
    void shouldHandleUnexpectedExceptionInPollLoop() {
        TestableMnsAlertConsumerLifecycle lifecycle = new TestableMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingAlertRoutingService()
        );
        lifecycle.runtimeExceptionToThrow = new IllegalStateException("boom");
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
    }

    @Test
    void shouldDelegatePopAndAcknowledgeToQueue() {
        Message message = message("msg-1", "rh-1", "{}");
        DelegateAwareMnsAlertConsumerLifecycle lifecycle = new DelegateAwareMnsAlertConsumerLifecycle(
                new ConsumerProperties(1, 2, 5),
                new RecordingExecutorService(),
                objectMapper(),
                new RecordingAlertRoutingService(),
                List.of(message)
        );

        assertThat(lifecycle.popMessages()).containsExactly(message);
        lifecycle.acknowledgeMessage(message);

        assertThat(lifecycle.batchSizeUsed).isEqualTo(2);
        assertThat(lifecycle.waitSecondsUsed).isEqualTo(5);
        assertThat(lifecycle.deletedReceiptHandle).isEqualTo("rh-1");
    }

    @Test
    void shouldExecuteConcreteQueueDelegateMethods() {
        MnsAlertConsumerLifecycle lifecycle = new MnsAlertConsumerLifecycle(
                null,
                new ConsumerProperties(1, 2, 5),
                new RecordingExecutorService(),
                objectMapper(),
                new RecordingAlertRoutingService()
        );

        assertThatThrownBy(() -> lifecycle.batchPopMessage(2, 5))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> lifecycle.deleteMessage("rh-1"))
                .isInstanceOf(NullPointerException.class);
    }

    private ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

    private Message message(String messageId, String receiptHandle, String body) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setReceiptHandle(receiptHandle);
        message.setMessageBodyAsRawString(body);
        return message;
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

    private static final class RecordingAlertRoutingService extends AlertRoutingService {

        private final List<AlertEvent> routedAlerts = new ArrayList<>();

        private RecordingAlertRoutingService() {
            super(
                    new AlertRoutingProperties(null, null, null),
                    List.of(new TelegramAlertSender(), new EmailAlertSender()),
                    new AlertMetricsService(
                            new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                            "alert-handler",
                            "pod-1",
                            "node-1"
                    )
            );
        }

        @Override
        public void route(AlertEvent alertEvent) {
            routedAlerts.add(alertEvent);
        }
    }

    private static final String MESSAGE_NOT_EXIST_ERROR_CODE = "MessageNotExist";

    private static final class TestableMnsAlertConsumerLifecycle extends MnsAlertConsumerLifecycle {

        private List<Message> messagesToPop = List.of();
        private final List<String> acknowledgedReceiptHandles = new ArrayList<>();
        private int popInvocationCount;
        private ClientException clientExceptionToThrow;
        private RuntimeException runtimeExceptionToThrow;

        private TestableMnsAlertConsumerLifecycle(
                ConsumerProperties properties,
                ObjectMapper objectMapper,
                AlertRoutingService alertRoutingService
        ) {
            super(null, properties, new RecordingExecutorService(), objectMapper, alertRoutingService);
        }

        @Override
        List<Message> popMessages() {
            popInvocationCount++;
            setRunning(false);
            if (clientExceptionToThrow != null) {
                throw clientExceptionToThrow;
            }
            if (runtimeExceptionToThrow != null) {
                throw runtimeExceptionToThrow;
            }
            return messagesToPop;
        }

        @Override
        void acknowledgeMessage(Message message) {
            acknowledgedReceiptHandles.add(message.getReceiptHandle());
        }

        private void setRunning(boolean value) {
            if (value) {
                start();
            } else {
                stop();
            }
        }
    }

    private static final class DelegateAwareMnsAlertConsumerLifecycle extends MnsAlertConsumerLifecycle {

        private final List<Message> messages;
        private int batchSizeUsed;
        private int waitSecondsUsed;
        private String deletedReceiptHandle;

        private DelegateAwareMnsAlertConsumerLifecycle(
                ConsumerProperties properties,
                ExecutorService executorService,
                ObjectMapper objectMapper,
                AlertRoutingService alertRoutingService,
                List<Message> messages
        ) {
            super(null, properties, executorService, objectMapper, alertRoutingService);
            this.messages = messages;
        }

        @Override
        List<Message> batchPopMessage(int batchSize, int waitSeconds) {
            batchSizeUsed = batchSize;
            waitSecondsUsed = waitSeconds;
            return messages;
        }

        @Override
        void deleteMessage(String receiptHandle) {
            deletedReceiptHandle = receiptHandle;
        }
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {

        private final List<Runnable> submittedTasks = new ArrayList<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            submittedTasks.add(command);
        }
    }
}
