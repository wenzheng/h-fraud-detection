package com.vincent.fraud.ingest.messaging;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.ingest.service.PublishTransactionCommand;
import com.vincent.fraud.ingest.service.PublishedTransaction;
import com.vincent.fraud.ingest.service.TransactionPublisher;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MnsTransactionPublisher implements TransactionPublisher {

    private final CloudQueue transactionQueue;
    private final ObjectMapper objectMapper;

    public MnsTransactionPublisher(CloudQueue transactionQueue, ObjectMapper objectMapper) {
        this.transactionQueue = transactionQueue;
        this.objectMapper = objectMapper;
    }

    @Override
    public PublishedTransaction publish(PublishTransactionCommand command) {
        Instant receivedAt = Instant.now();
        TransactionEvent event = new TransactionEvent(
                UUID.randomUUID().toString(),
                command.accountId(),
                command.merchantId(),
                command.deviceId(),
                command.ipAddress(),
                command.currency(),
                command.amount(),
                command.occurredAt(),
                receivedAt
        );

        Message message = new Message();
        message.setMessageBodyAsRawString(serialize(event));
        try {
            Message result = putMessage(message);
            return new PublishedTransaction(event.transactionId(), result.getMessageId(), receivedAt);
        } catch (ClientException exception) {
            throw new IllegalStateException("Failed to publish transaction to Alibaba Cloud SMQ", exception);
        }
    }

    Message putMessage(Message message) {
        return transactionQueue.putMessage(message);
    }

    private String serialize(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize transaction event", exception);
        }
    }
}
