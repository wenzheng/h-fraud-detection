package com.vincent.fraud.repository;

import com.vincent.fraud.model.Alert;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class AlertRepository {

    private final CopyOnWriteArrayList<Alert> alerts = new CopyOnWriteArrayList<>();

    public void save(Alert alert) {
        alerts.add(alert);
    }

    public List<Alert> findAll() {
        return List.copyOf(alerts);
    }
}
