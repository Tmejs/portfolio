package com.portfolio.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event representing account changes in the banking system.
 * This event is published when accounts are created, updated, or status changes occur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEvent {
    
    /**
     * Unique account identifier
     */
    private String accountId;
    
    /**
     * User ID who owns this account
     */
    private String userId;
    
    /**
     * Account type (CHECKING, SAVINGS, etc.)
     */
    private String accountType;
    
    /**
     * Current account balance
     */
    private BigDecimal balance;
    
    /**
     * Account status (ACTIVE, INACTIVE, CLOSED, etc.)
     */
    private String status;
    
    /**
     * Account opening date
     */
    private LocalDateTime openedDate;
    
    /**
     * Event type - CREATED, UPDATED, DELETED, STATUS_CHANGED
     */
    private String eventType;
    
    /**
     * Previous account status (for status change events)
     */
    private String previousStatus;
    
    /**
     * Previous balance (for balance updates)
     */
    private BigDecimal previousBalance;
    
    /**
     * Source system that generated this event
     */
    private String source;
    
    /**
     * Event timestamp when this event was published
     */
    private LocalDateTime eventTimestamp;
}
