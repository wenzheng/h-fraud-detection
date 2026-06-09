package com.vincent.fraud.ingest.controller;

import com.vincent.fraud.ingest.service.PublishedTransaction;
import com.vincent.fraud.ingest.service.TransactionPublisherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionPublisherService transactionPublisherService;

    public TransactionController(TransactionPublisherService transactionPublisherService) {
        this.transactionPublisherService = transactionPublisherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse submit(@Valid @RequestBody TransactionRequest request) {
        PublishedTransaction published = transactionPublisherService.publish(request.toCommand());
        return new TransactionResponse(published.transactionId(), published.queueMessageId(), "QUEUED");
    }

    public record TransactionResponse(String transactionId, String queueMessageId, String status) {
    }
}
