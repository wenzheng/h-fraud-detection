package com.vincent.fraud.ingest.service;

import org.springframework.stereotype.Service;

@Service
public class TransactionPublisherService {

    private final TransactionPublisher transactionPublisher;

    public TransactionPublisherService(TransactionPublisher transactionPublisher) {
        this.transactionPublisher = transactionPublisher;
    }

    public PublishedTransaction publish(PublishTransactionCommand command) {
        return transactionPublisher.publish(command);
    }
}
