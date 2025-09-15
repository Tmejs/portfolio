package com.portfolio.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event representing a transaction occurrence in the banking system.
 * This event is published by the banking service when transactions are processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    
    /**
     * Unique transaction identifier
     */
    private String transactionId;
    
    /**
     * Account ID associated with this transaction
     */
    private String accountId;
    
    /**
     * Transaction amount (positive for deposits, negative for withdrawals)
     */
    private BigDecimal amount;
    
    /**
     * Transaction type (DEPOSIT, WITHDRAWAL, TRANSFER, etc.)
     */
    private String transactionType;
    
    /**
     * Transaction category (GROCERY, RESTAURANT, FUEL, etc.)
     */
    private String category;
    
    /**
     * Timestamp when the transaction occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Transaction description or memo
     */
    private String description;
    
    /**
     * Merchant name or payee
     */
    private String merchant;
    
    /**
     * Account balance after this transaction
     */
    private BigDecimal balanceAfter;
    
    /**
     * Event type - CREATED, UPDATED, DELETED
     */
    private String eventType;
    
    /**
     * Source system that generated this event
     */
    private String source;
    
    /**
     * Event timestamp when this event was published
     */
    private LocalDateTime eventTimestamp;
}
