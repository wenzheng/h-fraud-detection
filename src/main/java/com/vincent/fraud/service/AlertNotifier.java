package com.vincent.fraud.service;

import com.vincent.fraud.model.Alert;
import com.vincent.fraud.model.Transaction;

public interface AlertNotifier {

    void notify(Alert alert, Transaction transaction);
}
