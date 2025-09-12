package com.portfolio.demo.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.portfolio.demo.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message DTO for transaction creation events
 * Used for JMS messaging when a transaction is created
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedMessage {
    
    private Long transactionId;
    private String referenceNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Account information
    private Long accountId;
    private String accountNumber;
    private String customerName;
    
    // Event metadata
    @Builder.Default
    private String eventType = "TRANSACTION_CREATED";
    @Builder.Default
    private String eventVersion = "1.0";
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTimestamp;
}
