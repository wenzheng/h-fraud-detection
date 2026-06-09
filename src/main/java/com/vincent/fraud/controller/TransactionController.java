package com.vincent.fraud.controller;

import com.vincent.fraud.model.Alert;
import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.service.AlertQueryService;
import com.vincent.fraud.service.TransactionIngestionService;
import com.vincent.fraud.service.TransactionQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionIngestionService ingestionService;
    private final TransactionQueryService transactionQueryService;
    private final AlertQueryService alertQueryService;

    public TransactionController(
            TransactionIngestionService ingestionService,
            TransactionQueryService transactionQueryService,
            AlertQueryService alertQueryService
    ) {
        this.ingestionService = ingestionService;
        this.transactionQueryService = transactionQueryService;
        this.alertQueryService = alertQueryService;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse submitTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = ingestionService.ingest(request.toCommand());
        return TransactionResponse.from(transaction);
    }

    @GetMapping("/transactions/{id}")
    public TransactionView getTransaction(@PathVariable String id) {
        return TransactionView.from(transactionQueryService.get(id));
    }

    @GetMapping("/alerts")
    public List<AlertView> getAlerts() {
        return alertQueryService.list().stream()
                .map(AlertView::from)
                .toList();
    }

    public record TransactionResponse(String id, String status) {
        static TransactionResponse from(Transaction transaction) {
            return new TransactionResponse(transaction.id(), transaction.status().name());
        }
    }

    public record TransactionView(
            String id,
            String accountId,
            String merchantId,
            String deviceId,
            String ipAddress,
            String currency,
            String status
    ) {
        static TransactionView from(Transaction transaction) {
            return new TransactionView(
                    transaction.id(),
                    transaction.accountId(),
                    transaction.merchantId(),
                    transaction.deviceId(),
                    transaction.ipAddress(),
                    transaction.currency(),
                    transaction.status().name()
            );
        }
    }

    public record AlertView(
            String transactionId,
            String accountId,
            List<String> reasons,
            String severity
    ) {
        static AlertView from(Alert alert) {
            return new AlertView(alert.transactionId(), alert.accountId(), alert.reasons(), alert.severity().name());
        }
    }
}
