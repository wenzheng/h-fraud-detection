package com.vincent.fraud.ingest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.ingest.controller.TransactionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TcpTransactionRequestHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final TransactionPublisherService transactionPublisherService;

    public TcpTransactionRequestHandler(
            ObjectMapper objectMapper,
            Validator validator,
            TransactionPublisherService transactionPublisherService
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.transactionPublisherService = transactionPublisherService;
    }

    public String handle(String payload) {
        try {
            TransactionRequest request = objectMapper.readValue(payload, TransactionRequest.class);
            Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                return writeResponse(new ErrorResponse("VALIDATION_ERROR", formatViolations(violations)));
            }

            PublishedTransaction published = transactionPublisherService.publish(request.toCommand());
            return writeResponse(new SuccessResponse(
                    published.transactionId(),
                    published.queueMessageId(),
                    "QUEUED"
            ));
        } catch (JsonProcessingException exception) {
            return writeResponse(new ErrorResponse("INVALID_JSON", exception.getOriginalMessage()));
        } catch (Exception exception) {
            return writeResponse(new ErrorResponse("PUBLISH_ERROR", exception.getMessage()));
        }
    }

    private String formatViolations(Set<ConstraintViolation<TransactionRequest>> violations) {
        return violations.stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
    }

    private String writeResponse(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TCP response", exception);
        }
    }

    record SuccessResponse(String transactionId, String queueMessageId, String status) {
    }

    record ErrorResponse(String error, String message) {
    }
}
