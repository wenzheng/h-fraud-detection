package com.vincent.fraud.processor.service;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.processor.config.ConsumerProperties;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class MnsConsumerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MnsConsumerLifecycle.class);
    private static final String MESSAGE_NOT_EXIST = "MessageNotExist";

    private final CloudQueue transactionQueue;
    private final ConsumerProperties properties;
    private final ExecutorService consumerExecutorService;
    private final ObjectMapper objectMapper;
    private final FraudProcessingService fraudProcessingService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MnsConsumerLifecycle(
            @Qualifier("transactionQueue") CloudQueue transactionQueue,
            ConsumerProperties properties,
            ExecutorService consumerExecutorService,
            ObjectMapper objectMapper,
            FraudProcessingService fraudProcessingService
    ) {
        this.transactionQueue = transactionQueue;
        this.properties = properties;
        this.consumerExecutorService = consumerExecutorService;
        this.objectMapper = objectMapper;
        this.fraudProcessingService = fraudProcessingService;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            for (int i = 0; i < properties.pollerConcurrency(); i++) {
                consumerExecutorService.submit(this::pollLoop);
            }
        }
    }

    void pollLoop() {
        while (running.get()) {
            try {
                List<Message> messages = popMessages();
                if (messages == null || messages.isEmpty()) {
                    continue;
                }
                for (Message message : messages) {
                    processMessage(message);
                }
            } catch (ClientException exception) {
                if (isQueueEmpty(exception)) {
                    log.debug("No transaction messages available in Alibaba Cloud SMQ");
                    continue;
                }
                log.error("Failed to poll messages from Alibaba Cloud SMQ", exception);
            } catch (Exception exception) {
                log.error("Unexpected error in MNS consumer loop", exception);
            }
        }
    }

    void processMessage(Message message) {
        try {
            TransactionEvent event = objectMapper.readValue(message.getMessageBodyAsRawString(), TransactionEvent.class);
            fraudProcessingService.process(event);
            acknowledgeMessage(message);
        } catch (Exception exception) {
            log.error("Failed to process messageId={} receiptHandle={}",
                    message.getMessageId(), message.getReceiptHandle(), exception);
        }
    }

    List<Message> popMessages() {
        return transactionQueue.batchPopMessage(
                properties.batchSize(),
                properties.waitSeconds()
        );
    }

    void acknowledgeMessage(Message message) {
        transactionQueue.deleteMessage(message.getReceiptHandle());
    }

    boolean isQueueEmpty(ClientException exception) {
        return MESSAGE_NOT_EXIST.equals(exception.getErrorCode())
                || (exception.getMessage() != null && exception.getMessage().contains(MESSAGE_NOT_EXIST));
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
