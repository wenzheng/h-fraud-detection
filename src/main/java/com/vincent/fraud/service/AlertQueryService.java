package com.vincent.fraud.service;

import com.vincent.fraud.model.Alert;
import com.vincent.fraud.repository.AlertRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertQueryService {

    private final AlertRepository alertRepository;

    public AlertQueryService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> list() {
        return alertRepository.findAll();
    }
}
