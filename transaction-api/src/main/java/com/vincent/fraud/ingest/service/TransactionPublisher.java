package com.vincent.fraud.ingest.service;

public interface TransactionPublisher {

    PublishedTransaction publish(PublishTransactionCommand command);
}
