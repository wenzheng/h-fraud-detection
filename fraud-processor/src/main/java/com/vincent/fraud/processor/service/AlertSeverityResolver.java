package com.vincent.fraud.processor.service;

import com.vincent.fraud.shared.model.AlertEvent;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AlertSeverityResolver {

    public AlertEvent.Severity resolve(List<String> reasons) {
        if (reasons.size() > 1) {
            return AlertEvent.Severity.HIGH;
        }

        String reason = reasons.isEmpty() ? "" : reasons.get(0).toLowerCase();
        if (reason.contains("watchlist") || reason.contains("threshold")) {
            if (reason.contains("merchant")) {
                return AlertEvent.Severity.MEDIUM;
            }
            return AlertEvent.Severity.HIGH;
        }
        if (reason.contains("merchant")) {
            return AlertEvent.Severity.MEDIUM;
        }
        return AlertEvent.Severity.LOW;
    }
}
