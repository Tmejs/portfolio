package com.portfolio.demo.service.messaging;

import com.portfolio.demo.dto.message.AccountCreatedMessage;
import com.portfolio.demo.dto.message.TransactionCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for publishing Kafka messages for banking events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProducerService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${banking.messaging.kafka.topics.account-created}")
    private String accountCreatedTopic;
    
    @Value("${banking.messaging.kafka.topics.transaction-created}")
    private String transactionCreatedTopic;
    
    /**
     * Publishes an account created event
     * @param message the account created message
     */
    public void publishAccountCreated(AccountCreatedMessage message) {
        try {
            // Set event timestamp if not already set
            if (message.getEventTimestamp() == null) {
                message.setEventTimestamp(LocalDateTime.now());
            }
            
            log.info("Publishing account created event for account: {}", message.getAccountNumber());
            kafkaTemplate.send(accountCreatedTopic, message.getAccountNumber(), message);
            log.debug("Account created event published successfully to topic: {}", accountCreatedTopic);
            
        } catch (Exception e) {
            log.error("Error publishing account created message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish account created event", e);
        }
    }
    
    /**
     * Publishes a transaction created event
     * @param message the transaction created message
     */
    public void publishTransactionCreated(TransactionCreatedMessage message) {
        try {
            // Set event timestamp if not already set
            if (message.getEventTimestamp() == null) {
                message.setEventTimestamp(LocalDateTime.now());
            }
            
            log.info("Publishing transaction created event for reference: {}", message.getReferenceNumber());
            kafkaTemplate.send(transactionCreatedTopic, message.getReferenceNumber(), message);
            log.debug("Transaction created event published successfully to topic: {}", transactionCreatedTopic);
            
        } catch (Exception e) {
            log.error("Error publishing transaction created message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish transaction created event", e);
        }
    }
}
