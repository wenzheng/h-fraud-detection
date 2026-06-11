package com.vincent.fraud.ingest.service;

import org.springframework.stereotype.Service;

@Service
public class TransactionPublisherService {

    private final TransactionPublisher transactionPublisher;
    private final IngressMetricsService ingressMetricsService;

    public TransactionPublisherService(
            TransactionPublisher transactionPublisher,
            IngressMetricsService ingressMetricsService
    ) {
        this.transactionPublisher = transactionPublisher;
        this.ingressMetricsService = ingressMetricsService;
    }

    public PublishedTransaction publish(PublishTransactionCommand command) {
        PublishedTransaction publishedTransaction = transactionPublisher.publish(command);
        ingressMetricsService.incrementPublishedCount();
        return publishedTransaction;
    }
}
