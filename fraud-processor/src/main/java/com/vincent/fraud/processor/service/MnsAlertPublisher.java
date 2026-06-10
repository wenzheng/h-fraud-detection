package com.vincent.fraud.processor.service;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.shared.model.AlertEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MnsAlertPublisher implements AlertPublisher {

    private final CloudQueue alertQueue;
    private final ObjectMapper objectMapper;

    public MnsAlertPublisher(@Qualifier("alertQueue") CloudQueue alertQueue, ObjectMapper objectMapper) {
        this.alertQueue = alertQueue;
        this.objectMapper = objectMapper;
    }

    @Override
    public String publish(AlertEvent alertEvent) {
        Message message = new Message();
        message.setMessageBodyAsRawString(serialize(alertEvent));
        return putMessage(message).getMessageId();
    }

    Message putMessage(Message message) {
        return alertQueue.putMessage(message);
    }

    private String serialize(AlertEvent alertEvent) {
        try {
            return objectMapper.writeValueAsString(alertEvent);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize alert event", exception);
        }
    }
}
