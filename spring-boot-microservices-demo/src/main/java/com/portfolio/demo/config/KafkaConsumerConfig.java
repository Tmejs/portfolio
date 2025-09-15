package com.portfolio.demo.config;

import com.portfolio.demo.dto.message.AccountCreatedMessage;
import com.portfolio.demo.dto.message.TransactionCreatedMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Kafka consumers with specific message type handling
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Consumer factory for AccountCreatedMessage
     */
    @Bean
    public ConsumerFactory<String, AccountCreatedMessage> accountCreatedConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-account");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.demo.dto.message");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AccountCreatedMessage.class.getName());

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for AccountCreatedMessage
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AccountCreatedMessage> accountCreatedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AccountCreatedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(accountCreatedConsumerFactory());
        return factory;
    }

    /**
     * Consumer factory for TransactionCreatedMessage
     */
    @Bean
    public ConsumerFactory<String, TransactionCreatedMessage> transactionCreatedConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-transaction");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.demo.dto.message");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionCreatedMessage.class.getName());

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for TransactionCreatedMessage
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedMessage> transactionCreatedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionCreatedConsumerFactory());
        return factory;
    }
}
