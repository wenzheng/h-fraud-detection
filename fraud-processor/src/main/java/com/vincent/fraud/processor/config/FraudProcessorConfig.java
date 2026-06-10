package com.vincent.fraud.processor.config;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.vincent.fraud.shared.mns.MnsEndpointResolver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        MnsProperties.class,
        FraudRulesProperties.class,
        ConsumerProperties.class
})
public class FraudProcessorConfig {

    private static final Logger log = LoggerFactory.getLogger(FraudProcessorConfig.class);

    @Bean(destroyMethod = "close")
    MNSClient mnsClient(MnsProperties properties) {
        String endpoint = MnsEndpointResolver.resolve(properties.endpoint());
        log.info("Creating MNS client for fraud-processor with endpoint={}", endpoint);
        CloudAccount account = new CloudAccount(
                properties.accessKeyId(),
                properties.accessKeySecret(),
                endpoint
                );
        return account.getMNSClient();
    }

    @Bean
    CloudQueue transactionQueue(MNSClient mnsClient, MnsProperties properties) {
        return mnsClient.getQueueRef(properties.transactionQueueName());
    }

    @Bean
    CloudQueue alertQueue(MNSClient mnsClient, MnsProperties properties) {
        return mnsClient.getQueueRef(properties.alertQueueName());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService consumerExecutorService(ConsumerProperties properties) {
        return Executors.newFixedThreadPool(properties.pollerConcurrency());
    }
}
