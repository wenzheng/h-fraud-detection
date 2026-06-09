package com.vincent.fraud.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FraudProperties.class)
public class FraudConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService transactionProcessorExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
