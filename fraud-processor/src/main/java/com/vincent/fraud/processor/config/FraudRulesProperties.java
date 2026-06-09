package com.vincent.fraud.processor.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fraud")
public record FraudRulesProperties(
        BigDecimal amountThreshold,
        List<String> suspiciousAccounts,
        List<String> suspiciousMerchants
) {
}
