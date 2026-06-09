package com.vincent.fraud.service;

import com.vincent.fraud.model.Transaction;
import com.vincent.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;

    public TransactionQueryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction get(String id) {
        return transactionRepository.get(id);
    }
}
