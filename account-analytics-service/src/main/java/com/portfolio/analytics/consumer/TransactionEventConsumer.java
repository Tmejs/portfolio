package com.portfolio.analytics.consumer;

import com.portfolio.analytics.event.TransactionEvent;
import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.service.AccountAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Kafka consumer that processes transaction events and updates account analytics in real-time.
 * This consumer maintains analytics cache freshness by recomputing metrics when transactions occur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final AccountAnalyticsService accountAnalyticsService;

    /**
     * Consumes transaction events and updates analytics accordingly.
     * Uses retryable topic for fault tolerance with exponential backoff.
     */
    @KafkaListener(
        topics = "${kafka.topics.transaction-events:transaction-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
        include = {Exception.class}
    )
    public void handleTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Processing transaction event: transactionId={}, accountId={}, amount={}, type={}", 
                    event.getTransactionId(), event.getAccountId(), event.getAmount(), event.getEventType());
            
            // Validate event data
            if (!isValidTransactionEvent(event)) {
                log.warn("Invalid transaction event received: {}", event);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the event based on event type
            switch (event.getEventType().toUpperCase()) {
                case "CREATED":
                    handleTransactionCreated(event);
                    break;
                case "UPDATED":
                    handleTransactionUpdated(event);
                    break;
                case "DELETED":
                    handleTransactionDeleted(event);
                    break;
                default:
                    log.warn("Unknown transaction event type: {}", event.getEventType());
            }
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.debug("Successfully processed transaction event: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing transaction event: transactionId={}, accountId={}, error={}", 
                    event.getTransactionId(), event.getAccountId(), e.getMessage(), e);
            throw e; // Let the retry mechanism handle it
        }
    }

    /**
     * Handles new transaction creation by updating analytics incrementally or recomputing.
     */
    private void handleTransactionCreated(TransactionEvent event) {
        String accountId = event.getAccountId();
        
        try {
            // Check if analytics exist for this account
            Optional<AccountAnalytics> existingAnalytics = accountAnalyticsService.getAccountAnalytics(accountId);
            
            if (existingAnalytics.isPresent()) {
                // Update existing analytics incrementally for better performance
                updateAnalyticsIncremental(existingAnalytics.get(), event);
                log.info("Incrementally updated analytics for account: {}", accountId);
            } else {
                // No existing analytics - we'd need to fetch all transactions from banking service
                // For now, we'll create empty analytics and warm cache later
                log.info("No existing analytics found for account: {}, cache invalidation triggered", accountId);
                accountAnalyticsService.invalidateAnalytics(accountId);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle transaction created event for account: {}, error: {}", 
                    accountId, e.getMessage(), e);
            // Invalidate cache to ensure data consistency
            accountAnalyticsService.invalidateAnalytics(accountId);
        }
    }

    /**
     * Handles transaction updates by invalidating cache to trigger recomputation.
     */
    private void handleTransactionUpdated(TransactionEvent event) {
        String accountId = event.getAccountId();
        
        log.info("Transaction updated, invalidating analytics cache for account: {}", accountId);
        
        // For transaction updates, we invalidate the cache to ensure accuracy
        // The next request for analytics will trigger recomputation
        accountAnalyticsService.invalidateAnalytics(accountId);
    }

    /**
     * Handles transaction deletion by invalidating cache.
     */
    private void handleTransactionDeleted(TransactionEvent event) {
        String accountId = event.getAccountId();
        
        log.info("Transaction deleted, invalidating analytics cache for account: {}", accountId);
        
        // Invalidate cache for the affected account
        accountAnalyticsService.invalidateAnalytics(accountId);
    }

    /**
     * Updates analytics incrementally for better performance on frequent transactions.
     */
    private void updateAnalyticsIncremental(AccountAnalytics analytics, TransactionEvent event) {
        boolean analyticsChanged = false;
        
        // Update transaction count
        Long currentCount = analytics.getTransactionCount();
        if (currentCount != null) {
            analytics.setTransactionCount(currentCount + 1);
            analyticsChanged = true;
        }
        
        // Update totals based on transaction amount
        BigDecimal amount = event.getAmount();
        if (amount != null) {
            // Update balance
            if (analytics.getTotalBalance() != null) {
                analytics.setTotalBalance(analytics.getTotalBalance().add(amount));
                analyticsChanged = true;
            }
            
            // Update income/expenses
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                // Positive amount = income
                if (analytics.getTotalIncome() != null) {
                    analytics.setTotalIncome(analytics.getTotalIncome().add(amount));
                }
                analytics.setDepositCount((analytics.getDepositCount() != null ? analytics.getDepositCount() : 0) + 1);
                
                // Check for largest deposit
                if (analytics.getLargestDeposit() == null || amount.compareTo(analytics.getLargestDeposit()) > 0) {
                    analytics.setLargestDeposit(amount);
                }
            } else {
                // Negative amount = expense
                BigDecimal expenseAmount = amount.abs();
                if (analytics.getTotalExpenses() != null) {
                    analytics.setTotalExpenses(analytics.getTotalExpenses().add(expenseAmount));
                }
                analytics.setWithdrawalCount((analytics.getWithdrawalCount() != null ? analytics.getWithdrawalCount() : 0) + 1);
                
                // Check for largest withdrawal
                if (analytics.getLargestWithdrawal() == null || expenseAmount.compareTo(analytics.getLargestWithdrawal()) > 0) {
                    analytics.setLargestWithdrawal(expenseAmount);
                }
            }
        }
        
        // Update timestamps
        if (event.getTimestamp() != null) {
            LocalDateTime transactionDate = event.getTimestamp();
            
            // Update last transaction date
            if (analytics.getLastTransactionDate() == null || 
                transactionDate.isAfter(analytics.getLastTransactionDate())) {
                analytics.setLastTransactionDate(transactionDate);
                analyticsChanged = true;
            }
            
            // Update first transaction date
            if (analytics.getFirstTransactionDate() == null || 
                transactionDate.isBefore(analytics.getFirstTransactionDate())) {
                analytics.setFirstTransactionDate(transactionDate);
                analyticsChanged = true;
            }
        }
        
        // Update category information
        if (event.getCategory() != null && !event.getCategory().trim().isEmpty()) {
            // For complex category analytics, we may need to invalidate cache for full recomputation
            // This is a trade-off between performance and accuracy
            log.debug("Category information present, may need full recomputation for account: {}", 
                    analytics.getAccountId());
        }
        
        if (analyticsChanged) {
            // Update timestamps and save
            analytics.setLastUpdated(LocalDateTime.now());
            accountAnalyticsService.saveAccountAnalytics(analytics);
            
            log.info("Successfully updated analytics incrementally for account: {}", analytics.getAccountId());
        }
    }

    /**
     * Validates transaction event data for processing.
     */
    private boolean isValidTransactionEvent(TransactionEvent event) {
        if (event == null) {
            log.warn("Null transaction event received");
            return false;
        }
        
        if (event.getTransactionId() == null || event.getTransactionId().trim().isEmpty()) {
            log.warn("Transaction event missing transaction ID: {}", event);
            return false;
        }
        
        if (event.getAccountId() == null || event.getAccountId().trim().isEmpty()) {
            log.warn("Transaction event missing account ID: {}", event);
            return false;
        }
        
        if (event.getAmount() == null) {
            log.warn("Transaction event missing amount: {}", event);
            return false;
        }
        
        if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
            log.warn("Transaction event missing event type: {}", event);
            return false;
        }
        
        return true;
    }
}
