package com.vincent.fraud.queue;

import com.vincent.fraud.model.Transaction;

public interface TransactionQueue {

    void publish(Transaction transaction);
}
