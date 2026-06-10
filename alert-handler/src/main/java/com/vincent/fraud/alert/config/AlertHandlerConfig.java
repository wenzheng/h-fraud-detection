package com.vincent.fraud.alert.config;

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
        ConsumerProperties.class,
        AlertRoutingProperties.class
})
public class AlertHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(AlertHandlerConfig.class);

    @Bean(destroyMethod = "close")
    MNSClient mnsClient(MnsProperties properties) {
        String endpoint = MnsEndpointResolver.resolve(properties.endpoint());
        log.info("Creating MNS client for alert-handler with endpoint={}", endpoint);
        CloudAccount account = new CloudAccount(
                endpoint,
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
