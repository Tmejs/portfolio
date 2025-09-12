package com.portfolio.demo.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${banking.messaging.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${banking.messaging.kafka.topics.transaction-created}")
    private String transactionCreatedTopic;

    @Value("${banking.messaging.kafka.topics.transaction-transfer}")
    private String transactionTransferTopic;

    @Value("${banking.messaging.kafka.topics.partitions:3}")
    private int partitions;

    @Value("${banking.messaging.kafka.topics.replication-factor:1}")
    private short replicationFactor;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic accountCreatedTopic() {
        return new NewTopic(accountCreatedTopic, partitions, replicationFactor);
    }

    @Bean
    public NewTopic transactionCreatedTopic() {
        return new NewTopic(transactionCreatedTopic, partitions, replicationFactor);
    }

    @Bean
    public NewTopic transactionTransferTopic() {
        return new NewTopic(transactionTransferTopic, partitions, replicationFactor);
    }
}
