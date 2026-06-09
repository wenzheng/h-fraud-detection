package com.vincent.fraud.ingest.config;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MnsProperties.class)
public class TransactionApiConfig {

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
}
