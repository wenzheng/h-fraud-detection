package com.vincent.fraud.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aliyun.mns.common.ClientException;
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
import java.util.concurrent.ExecutorService;
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
                new FraudProcessingService(
                        new StubFraudDetectionService(),
                        new NoOpAlertService(),
                        metricsService()
                )
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

    @Test
    void shouldTreatMessageNotExistAsEmptyQueue() {
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingFraudProcessingService()
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
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingFraudProcessingService()
        );
        lifecycle.messagesToPop = List.of();
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
        assertThat(lifecycle.acknowledgedReceiptHandles).isEmpty();
    }

    @Test
    void shouldHandleUnexpectedClientExceptionInPollLoop() {
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingFraudProcessingService()
        );
        lifecycle.clientExceptionToThrow = new ClientException("boom", "req-1");
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
    }

    @Test
    void shouldHandleUnexpectedExceptionInPollLoop() {
        TestableMnsConsumerLifecycle lifecycle = new TestableMnsConsumerLifecycle(
                new ConsumerProperties(1, 1, 1),
                objectMapper(),
                new RecordingFraudProcessingService()
        );
        lifecycle.runtimeExceptionToThrow = new IllegalStateException("boom");
        lifecycle.setRunning(true);

        lifecycle.pollLoop();

        assertThat(lifecycle.popInvocationCount).isEqualTo(1);
    }

    @Test
    void shouldDelegatePopAndAcknowledgeToQueue() {
        Message message = message("msg-1", "rh-1", "{}");
        DelegateAwareMnsConsumerLifecycle lifecycle = new DelegateAwareMnsConsumerLifecycle(
                new ConsumerProperties(1, 2, 5),
                new RecordingExecutorService(),
                objectMapper(),
                new RecordingFraudProcessingService(),
                List.of(message)
        );

        assertThat(lifecycle.popMessages()).containsExactly(message);
        lifecycle.acknowledgeMessage(message);

        assertThat(lifecycle.batchSizeUsed).isEqualTo(2);
        assertThat(lifecycle.waitSecondsUsed).isEqualTo(5);
        assertThat(lifecycle.deletedReceiptHandle).isEqualTo("rh-1");
    }

    @Test
    void shouldExposePhaseZero() {
        MnsConsumerLifecycle lifecycle = new MnsConsumerLifecycle(
                null,
                new ConsumerProperties(1, 1, 1),
                new RecordingExecutorService(),
                objectMapper(),
                new RecordingFraudProcessingService()
        );

        assertThat(lifecycle.getPhase()).isZero();
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

    private static ProcessingMetricsService metricsService() {
        return new ProcessingMetricsService(
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                "fraud-processor",
                "pod-1",
                "node-1"
        );
    }

    private static final class RecordingFraudProcessingService extends FraudProcessingService {

        private final List<TransactionEvent> processedTransactions = new ArrayList<>();

        private RecordingFraudProcessingService() {
            super(new StubFraudDetectionService(), new NoOpAlertService(), metricsService());
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
        private ClientException clientExceptionToThrow;
        private RuntimeException runtimeExceptionToThrow;

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

    private static final class DelegateAwareMnsConsumerLifecycle extends MnsConsumerLifecycle {

        private final List<Message> messages;
        private int batchSizeUsed;
        private int waitSecondsUsed;
        private String deletedReceiptHandle;

        private DelegateAwareMnsConsumerLifecycle(
                ConsumerProperties properties,
                ExecutorService executorService,
                ObjectMapper objectMapper,
                FraudProcessingService fraudProcessingService,
                List<Message> messages
        ) {
            super(null, properties, executorService, objectMapper, fraudProcessingService);
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
