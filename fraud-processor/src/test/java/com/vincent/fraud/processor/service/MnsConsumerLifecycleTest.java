package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vincent.fraud.processor.config.ConsumerProperties;
import com.vincent.fraud.processor.detection.FraudDetectionService;
import com.vincent.fraud.shared.model.FraudDecision;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MnsConsumerLifecycleTest {

    @Test
    void shouldSubmitConfiguredNumberOfPollersOnStart() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        MnsConsumerLifecycle lifecycle = new MnsConsumerLifecycle(
                null,
                new ConsumerProperties(3, 1, 1),
                executorService,
                objectMapper(),
                new FraudProcessingService(new StubFraudDetectionService(), new NoOpAlertService())
        );

        lifecycle.start();

        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(executorService.submittedTasks).hasSize(3);
    }

    @Test
    void shouldProcessPoppedMessagesInPollLoop() throws Exception {
        RecordingFraudProcessingService processingService = new RecordingFraudProcessingService();
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 2, 5),
                objectMapper(),
                processingService
        );
        Message message = message("msg-1", "rh-1", objectMapper().writeValueAsString(transaction()));
        lifecycle.messagesToPop = List.of(message);
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(processingService.processedTransactions).hasSize(1);
        assertThat(processingService.processedTransactions.get(0).transactionId()).isEqualTo("tx-1");
        assertThat(lifecycle.acknowledgedReceiptHandles).containsExactly("rh-1");
        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
    }

    @Test
    void shouldAcknowledgeMessageWhenProcessSucceeds() throws Exception {
        RecordingFraudProcessingService processingService = new RecordingFraudProcessingService();
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                processingService
        );
        Message message = message("msg-1", "rh-1", objectMapper().writeValueAsString(transaction()));

        lifecycle.processMessage(message);

        assertThat(processingService.processedTransactions).hasSize(1);
        assertThat(lifecycle.acknowledgedReceiptHandles).containsExactly("rh-1");
    }

    @Test
    void shouldNotAcknowledgeMessageWhenProcessFails() {
        RecordingFraudProcessingService processingService = new RecordingFraudProcessingService();
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public <T> T readValue(String content, Class<T> valueType) {
                throw new IllegalStateException("boom");
            }
        };
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                failingObjectMapper,
                processingService
        );
        Message message = message("msg-1", "rh-1", "{bad json");

        lifecycle.processMessage(message);

        assertThat(processingService.processedTransactions).isEmpty();
        assertThat(lifecycle.acknowledgedReceiptHandles).isEmpty();
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

    private TransactionEvent transaction() {
        Instant now = Instant.parse("2026-06-10T01:02:03Z");
        return new TransactionEvent(
                "tx-1",
                "acct-1",
                "merchant-1",
                "device-1",
                "10.0.0.1",
                "USD",
                new BigDecimal("42.00"),
                now,
                now
        );
    }

    private static final class RecordingFraudProcessingService extends FraudProcessingService {

        private final List<TransactionEvent> processedTransactions = new ArrayList<>();

        private RecordingFraudProcessingService() {
            super(new StubFraudDetectionService(), new NoOpAlertService());
        }

        @Override
        public void process(TransactionEvent transactionEvent) {
            processedTransactions.add(transactionEvent);
        }
    }

    private static final class StubFraudDetectionService extends FraudDetectionService {

        private StubFraudDetectionService() {
            super(List.of());
        }

        @Override
        public FraudDecision evaluate(TransactionEvent transactionEvent) {
            return FraudDecision.approved();
        }
    }

    private static final class NoOpAlertService extends AlertService {

        private NoOpAlertService() {
            super(alertEvent -> "unused", new AlertSeverityResolver());
        }

        @Override
        public void raise(TransactionEvent transactionEvent, List<String> reasons) {
            // no-op
        }
    }

    private static final class TestableMnsConsumerLifecycle extends MnsConsumerLifecycle {

        private List<Message> messagesToPop = List.of();
        private final List<String> acknowledgedReceiptHandles = new ArrayList<>();
        private int popInvocationCount;

        private TestableMnsConsumerLifecycle(
                ConsumerProperties properties,
                ObjectMapper objectMapper,
                FraudProcessingService fraudProcessingService
        ) {
            super(null, properties, new RecordingExecutorService(), objectMapper, fraudProcessingService);
        }

        @Override
        List<Message> popMessages() {
            popInvocationCount++;
            setRunning(false);
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
