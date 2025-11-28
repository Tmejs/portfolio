package com.portfolio.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for transaction data shared across Portfolio microservices.
 * This class represents transaction information that can be exchanged between
 * banking service, analytics service, and other components.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {
    
    /**
     * Unique identifier for the transaction
     */
    @NotBlank(message = "Transaction ID cannot be blank")
    private String transactionId;
    
    /**
     * Transaction amount - positive for deposits, negative for withdrawals
     */
    @NotNull(message = "Transaction amount cannot be null")
    private BigDecimal amount;
    
    /**
     * Transaction category (e.g., FOOD, TRANSPORT, SALARY, etc.)
     */
    private String category;
    
    /**
     * Date and time when the transaction occurred
     */
    @NotNull(message = "Transaction date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDate;
    
    /**
     * Human-readable description of the transaction
     */
    private String description;
    
    /**
     * Account ID associated with this transaction
     */
    @NotBlank(message = "Account ID cannot be blank")
    private String accountId;
    
    /**
     * Reference ID for the transaction (e.g., external system reference)
     */
    private String referenceId;
    
    /**
     * Transaction type (e.g., PURCHASE, REFUND, TRANSFER, DEPOSIT, WITHDRAWAL)
     */
    private String type;
    
    /**
     * Merchant name or payee
     */
    private String merchant;
    
    /**
     * Currency code (e.g., USD, EUR)
     */
    @Builder.Default
    private String currency = "USD";
    
    /**
     * Balance after this transaction was processed
     */
    private BigDecimal balanceAfter;
}
