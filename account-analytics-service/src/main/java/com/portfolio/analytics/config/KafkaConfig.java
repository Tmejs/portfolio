package com.portfolio.analytics.config;

import com.portfolio.analytics.event.AccountEvent;
import com.portfolio.analytics.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for event-driven analytics processing.
 * Configures consumers for transaction and account events with proper error handling and retry logic.
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Consumer configuration for both transaction and account events.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Deserializer configuration
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        
        // JSON deserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        // Consumer performance settings
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);    // 30 seconds
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
        
        // Enable manual acknowledgment for better control
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Producer configuration for publishing events (if needed for notifications).
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Producer reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Idempotence for exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Kafka template for publishing events.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka listener container factory with error handling and retry logic.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Container properties
        ContainerProperties containerProps = factory.getContainerProperties();
        containerProps.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProps.setPollTimeout(3000);
        
        // Concurrency settings
        factory.setConcurrency(2); // Process messages in parallel
        
        // Error handling with exponential backoff
        factory.setCommonErrorHandler(createErrorHandler());
        
        log.info("Kafka listener container factory configured with concurrency: 2");
        
        return factory;
    }

    /**
     * Creates error handler with dead letter topic publishing and exponential backoff.
     */
    private DefaultErrorHandler createErrorHandler() {
        // Configure exponential backoff
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);  // 1 second
        backOff.setMultiplier(2.0);         // Double each time
        backOff.setMaxInterval(30000L);     // Max 30 seconds
        backOff.setMaxElapsedTime(300000L); // Max 5 minutes total
        
        // Dead letter publisher for failed messages
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate(),
            (consumerRecord, exception) -> {
                // Determine dead letter topic based on original topic
                String originalTopic = consumerRecord.topic();
                String dlqTopic = originalTopic + ".dlq";
                
                log.error("Publishing message to dead letter topic: {} -> {}, error: {}", 
                        originalTopic, dlqTopic, exception.getMessage());
                
                return new TopicPartition(dlqTopic, consumerRecord.partition());
            }
        );
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Add specific exceptions that should NOT be retried
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            com.fasterxml.jackson.core.JsonProcessingException.class
        );
        
        // Log retry attempts
        errorHandler.setRetryListeners((consumerRecord, ex, deliveryAttempt) -> {
            log.warn("Retry attempt {} for message from topic: {}, partition: {}, offset: {}, error: {}", 
                    deliveryAttempt, consumerRecord.topic(), consumerRecord.partition(), 
                    consumerRecord.offset(), ex.getMessage());
        });
        
        return errorHandler;
    }

    /**
     * Consumer factory specifically for transaction events.
     */
    @Bean
    public ConsumerFactory<String, TransactionEvent> transactionEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-transactions");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionEvent.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Consumer factory specifically for account events.
     */
    @Bean
    public ConsumerFactory<String, AccountEvent> accountEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-accounts");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AccountEvent.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // Commented out health indicator - will be added when actuator is properly configured
    // @Bean
    // public HealthIndicator kafkaHealthIndicator() {
    //     return () -> {
    //         try {
    //             consumerFactory();
    //             return Health.up()
    //                     .withDetail("kafka-brokers", bootstrapServers)
    //                     .withDetail("consumer-group", groupId)
    //                     .build();
    //         } catch (Exception e) {
    //             return Health.down()
    //                     .withDetail("error", e.getMessage())
    //                     .build();
    //         }
    //     };
    // }
}
