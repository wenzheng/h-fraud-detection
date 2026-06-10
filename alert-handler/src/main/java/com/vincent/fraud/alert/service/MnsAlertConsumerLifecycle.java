package com.vincent.fraud.alert.service;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.alert.config.ConsumerProperties;
import com.vincent.fraud.shared.model.AlertEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class MnsAlertConsumerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MnsAlertConsumerLifecycle.class);
    private static final String MESSAGE_NOT_EXIST = "MessageNotExist";

    private final CloudQueue alertQueue;
    private final ConsumerProperties properties;
    private final ExecutorService consumerExecutorService;
    private final ObjectMapper objectMapper;
    private final AlertRoutingService alertRoutingService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MnsAlertConsumerLifecycle(
            CloudQueue alertQueue,
            ConsumerProperties properties,
            ExecutorService consumerExecutorService,
            ObjectMapper objectMapper,
            AlertRoutingService alertRoutingService
    ) {
        this.alertQueue = alertQueue;
        this.properties = properties;
        this.consumerExecutorService = consumerExecutorService;
        this.objectMapper = objectMapper;
        this.alertRoutingService = alertRoutingService;
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
                    log.debug("No alert messages available in Alibaba Cloud SMQ");
                    continue;
                }
                log.error("Failed to poll alert messages from Alibaba Cloud SMQ", exception);
            } catch (Exception exception) {
                log.error("Unexpected error in alert consumer loop", exception);
            }
        }
    }

    void processMessage(Message message) {
        try {
            AlertEvent event = objectMapper.readValue(message.getMessageBodyAsRawString(), AlertEvent.class);
            alertRoutingService.route(event);
            acknowledgeMessage(message);
        } catch (Exception exception) {
            log.error("Failed to process alert messageId={} receiptHandle={}",
                    message.getMessageId(), message.getReceiptHandle(), exception);
        }
    }

    List<Message> popMessages() {
        return alertQueue.batchPopMessage(
                properties.batchSize(),
                properties.waitSeconds()
        );
    }

    void acknowledgeMessage(Message message) {
        alertQueue.deleteMessage(message.getReceiptHandle());
    }

    boolean isQueueEmpty(ClientException exception) {
        return MESSAGE_NOT_EXIST.equals(exception.getErrorCode());
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
