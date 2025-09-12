package com.portfolio.demo.service.messaging;

import com.portfolio.demo.dto.message.AccountCreatedMessage;
import com.portfolio.demo.dto.message.TransactionCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service for consuming Kafka messages for banking events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumerService {
    
    /**
     * Consumes account created events
     * @param message the account created message
     */
    @KafkaListener(topics = "${banking.messaging.kafka.topics.account-created}", containerFactory = "accountCreatedKafkaListenerContainerFactory")
    public void handleAccountCreated(AccountCreatedMessage message) {
        try {
            log.info("Received account created event for account: {}", message.getAccountNumber());
            
            // Process the account created event
            processAccountCreated(message);
            
            log.info("Successfully processed account created event for account: {}", 
                    message.getAccountNumber());
            
        } catch (Exception e) {
            log.error("Error processing account created message for account: {} - Error: {}", 
                     message.getAccountNumber(), e.getMessage(), e);
            // In a production environment, you might want to send to a dead letter topic
            throw new RuntimeException("Failed to process account created event", e);
        }
    }
    
    /**
     * Consumes transaction created events
     * @param message the transaction created message
     */
    @KafkaListener(topics = "${banking.messaging.kafka.topics.transaction-created}", containerFactory = "transactionCreatedKafkaListenerContainerFactory")
    public void handleTransactionCreated(TransactionCreatedMessage message) {
        try {
            log.info("Received transaction created event for reference: {}", message.getReferenceNumber());
            
            // Process the transaction created event
            processTransactionCreated(message);
            
            log.info("Successfully processed transaction created event for reference: {}", 
                    message.getReferenceNumber());
            
        } catch (Exception e) {
            log.error("Error processing transaction created message for reference: {} - Error: {}", 
                     message.getReferenceNumber(), e.getMessage(), e);
            // In a production environment, you might want to send to a dead letter topic
            throw new RuntimeException("Failed to process transaction created event", e);
        }
    }
    
    /**
     * Business logic for processing account created events
     * @param message the account created message
     */
    private void processAccountCreated(AccountCreatedMessage message) {
        log.info("Processing account created event - Account: {}, Customer: {}, Type: {}, Balance: {}", 
                message.getAccountNumber(), 
                message.getCustomerName(), 
                message.getAccountType(), 
                message.getBalance());
        
        // Example processing logic:
        // - Send welcome email to customer
        // - Update analytics/reporting systems
        // - Trigger account setup workflows
        // - Log audit trail
        
        // For now, just log the event processing
        log.info("Account created event processed successfully for customer: {} with account: {}", 
                message.getCustomerName(), message.getAccountNumber());
    }
    
    /**
     * Business logic for processing transaction created events
     * @param message the transaction created message
     */
    private void processTransactionCreated(TransactionCreatedMessage message) {
        log.info("Processing transaction created event - Reference: {}, Type: {}, Amount: {}, Account: {}", 
                message.getReferenceNumber(), 
                message.getTransactionType(), 
                message.getAmount(), 
                message.getAccountNumber());
        
        // Example processing logic:
        // - Send transaction notifications
        // - Update fraud detection systems
        // - Trigger compliance checks for large amounts
        // - Update analytics/reporting systems
        // - Log audit trail
        
        // For now, just log the event processing
        log.info("Transaction created event processed successfully - Reference: {} for account: {}", 
                message.getReferenceNumber(), message.getAccountNumber());
    }
}
