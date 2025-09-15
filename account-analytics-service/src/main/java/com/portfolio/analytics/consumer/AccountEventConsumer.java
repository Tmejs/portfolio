package com.portfolio.analytics.consumer;

import com.portfolio.analytics.event.AccountEvent;
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

/**
 * Kafka consumer that processes account events and manages analytics lifecycle.
 * This consumer handles account creation, updates, and deletion events from the banking service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {

    private final AccountAnalyticsService accountAnalyticsService;

    /**
     * Consumes account events and manages analytics lifecycle accordingly.
     * Uses retryable topic for fault tolerance with exponential backoff.
     */
    @KafkaListener(
        topics = "${kafka.topics.account-events:account-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
        include = {Exception.class}
    )
    public void handleAccountEvent(
            @Payload AccountEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Processing account event: accountId={}, userId={}, eventType={}, status={}", 
                    event.getAccountId(), event.getUserId(), event.getEventType(), event.getStatus());
            
            // Validate event data
            if (!isValidAccountEvent(event)) {
                log.warn("Invalid account event received: {}", event);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the event based on event type
            switch (event.getEventType().toUpperCase()) {
                case "CREATED":
                    handleAccountCreated(event);
                    break;
                case "UPDATED":
                    handleAccountUpdated(event);
                    break;
                case "DELETED":
                    handleAccountDeleted(event);
                    break;
                case "STATUS_CHANGED":
                    handleAccountStatusChanged(event);
                    break;
                default:
                    log.warn("Unknown account event type: {}", event.getEventType());
            }
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.debug("Successfully processed account event: {}", event.getAccountId());
            
        } catch (Exception e) {
            log.error("Error processing account event: accountId={}, eventType={}, error={}", 
                    event.getAccountId(), event.getEventType(), e.getMessage(), e);
            throw e; // Let the retry mechanism handle it
        }
    }

    /**
     * Handles account creation by initializing analytics structure.
     * New accounts start with empty analytics that will be populated as transactions occur.
     */
    private void handleAccountCreated(AccountEvent event) {
        String accountId = event.getAccountId();
        
        try {
            log.info("New account created: accountId={}, userId={}, type={}", 
                    accountId, event.getUserId(), event.getAccountType());
            
            // Initialize analytics for the new account if needed
            // For now, we just ensure any existing cache is cleared
            // Analytics will be created on-demand when transactions start flowing
            accountAnalyticsService.invalidateAnalytics(accountId);
            
            log.info("Analytics lifecycle initialized for new account: {}", accountId);
            
        } catch (Exception e) {
            log.error("Failed to handle account created event for account: {}, error: {}", 
                    accountId, e.getMessage(), e);
            // Non-critical - analytics can be computed later
        }
    }

    /**
     * Handles account updates by invalidating cache to ensure fresh data.
     */
    private void handleAccountUpdated(AccountEvent event) {
        String accountId = event.getAccountId();
        
        log.info("Account updated, checking analytics impact for account: {}", accountId);
        
        try {
            // Check if the update affects analytics
            boolean shouldInvalidateCache = shouldInvalidateForAccountUpdate(event);
            
            if (shouldInvalidateCache) {
                log.info("Account update affects analytics, invalidating cache for account: {}", accountId);
                accountAnalyticsService.invalidateAnalytics(accountId);
            } else {
                log.debug("Account update does not affect analytics for account: {}", accountId);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle account updated event for account: {}, error: {}", 
                    accountId, e.getMessage(), e);
            // Err on the side of caution - invalidate cache
            accountAnalyticsService.invalidateAnalytics(accountId);
        }
    }

    /**
     * Handles account deletion by cleaning up all associated analytics data.
     */
    private void handleAccountDeleted(AccountEvent event) {
        String accountId = event.getAccountId();
        
        log.info("Account deleted, cleaning up analytics data for account: {}", accountId);
        
        try {
            // Delete all analytics data for this account
            if (accountAnalyticsService.analyticsExist(accountId)) {
                accountAnalyticsService.deleteAnalytics(accountId);
                log.info("Successfully deleted analytics data for account: {}", accountId);
            } else {
                log.debug("No analytics data found to delete for account: {}", accountId);
            }
            
            // Also ensure cache is cleared
            accountAnalyticsService.invalidateAnalytics(accountId);
            
        } catch (Exception e) {
            log.error("Failed to clean up analytics for deleted account: {}, error: {}", 
                    accountId, e.getMessage(), e);
            // Still try to invalidate cache
            try {
                accountAnalyticsService.invalidateAnalytics(accountId);
            } catch (Exception cacheError) {
                log.error("Failed to invalidate cache for deleted account: {}, error: {}", 
                        accountId, cacheError.getMessage());
            }
        }
    }

    /**
     * Handles account status changes that may affect analytics computation.
     */
    private void handleAccountStatusChanged(AccountEvent event) {
        String accountId = event.getAccountId();
        String newStatus = event.getStatus();
        String previousStatus = event.getPreviousStatus();
        
        log.info("Account status changed: accountId={}, from={} to={}", 
                accountId, previousStatus, newStatus);
        
        try {
            // Determine if status change affects analytics
            boolean shouldInvalidateCache = shouldInvalidateForStatusChange(newStatus, previousStatus);
            
            if (shouldInvalidateCache) {
                log.info("Status change affects analytics, invalidating cache for account: {}", accountId);
                accountAnalyticsService.invalidateAnalytics(accountId);
                
                // If account is being closed or deactivated, we might want to preserve final analytics
                if (isAccountBeingClosed(newStatus)) {
                    log.info("Account being closed, preserving final analytics state for: {}", accountId);
                    // Analytics are preserved in the cache/database for historical purposes
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to handle account status change for account: {}, error: {}", 
                    accountId, e.getMessage(), e);
            // Err on the side of caution
            accountAnalyticsService.invalidateAnalytics(accountId);
        }
    }

    /**
     * Determines if an account update should trigger analytics cache invalidation.
     */
    private boolean shouldInvalidateForAccountUpdate(AccountEvent event) {
        // Invalidate if balance changed significantly (could indicate transaction processing)
        if (event.getBalance() != null && event.getPreviousBalance() != null) {
            // If balance changed, likely due to transactions that may not have triggered events yet
            return !event.getBalance().equals(event.getPreviousBalance());
        }
        
        // For other account updates (like name, type changes), analytics may not be affected
        return false;
    }

    /**
     * Determines if a status change should trigger analytics cache invalidation.
     */
    private boolean shouldInvalidateForStatusChange(String newStatus, String previousStatus) {
        if (newStatus == null || previousStatus == null) {
            return true; // Be safe
        }
        
        // Status changes that affect transaction processing should invalidate cache
        String[] statusesAffectingTransactions = {"ACTIVE", "INACTIVE", "FROZEN", "CLOSED"};
        
        for (String status : statusesAffectingTransactions) {
            if (status.equalsIgnoreCase(newStatus) || status.equalsIgnoreCase(previousStatus)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determines if an account is being closed or deactivated.
     */
    private boolean isAccountBeingClosed(String status) {
        if (status == null) {
            return false;
        }
        
        return "CLOSED".equalsIgnoreCase(status) || 
               "TERMINATED".equalsIgnoreCase(status) ||
               "SUSPENDED".equalsIgnoreCase(status);
    }

    /**
     * Validates account event data for processing.
     */
    private boolean isValidAccountEvent(AccountEvent event) {
        if (event == null) {
            log.warn("Null account event received");
            return false;
        }
        
        if (event.getAccountId() == null || event.getAccountId().trim().isEmpty()) {
            log.warn("Account event missing account ID: {}", event);
            return false;
        }
        
        if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
            log.warn("Account event missing event type: {}", event);
            return false;
        }
        
        return true;
    }
}
