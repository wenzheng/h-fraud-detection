package com.vincent.fraud.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraud")
public record FraudProperties(
        BigDecimal amountThreshold,
        int velocityCountThreshold,
        Duration velocityWindow,
        List<String> suspiciousAccounts
) {
}
