package com.vincent.fraud.alert.config;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        MnsProperties.class,
        ConsumerProperties.class,
        AlertRoutingProperties.class
})
public class AlertHandlerConfig {

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
    CloudQueue alertQueue(MNSClient mnsClient, MnsProperties properties) {
        return mnsClient.getQueueRef(properties.alertQueueName());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService consumerExecutorService(ConsumerProperties properties) {
        return Executors.newFixedThreadPool(properties.pollerConcurrency());
    }
}
