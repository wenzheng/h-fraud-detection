package com.vincent.fraud.processor.config;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        MnsProperties.class,
        FraudRulesProperties.class,
        ConsumerProperties.class,
        TelegramProperties.class
})
public class FraudProcessorConfig {

    @Bean(destroyMethod = "close")
    MNSClient mnsClient(MnsProperties properties) {
        CloudAccount account = new CloudAccount(
                properties.endpoint(),
                properties.accessKeyId(),
                properties.accessKeySecret()
        );
        return account.getMNSClient();
    }

    @Bean
    CloudQueue transactionQueue(MNSClient mnsClient, MnsProperties properties) {
        return mnsClient.getQueueRef(properties.queueName());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService consumerExecutorService(ConsumerProperties properties) {
        return Executors.newFixedThreadPool(properties.pollerConcurrency());
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
