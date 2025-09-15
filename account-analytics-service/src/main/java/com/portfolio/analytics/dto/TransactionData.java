package com.portfolio.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing transaction data used for computing account analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {
    
    /**
     * Unique transaction identifier
     */
    @NotBlank(message = "Transaction ID cannot be blank")
    private String id;
    
    /**
     * Transaction amount (positive for credits, negative for debits)
     */
    @NotNull(message = "Transaction amount cannot be null")
    private BigDecimal amount;
    
    /**
     * Transaction category (e.g., GROCERY, RESTAURANT, FUEL)
     */
    private String category;
    
    /**
     * Transaction timestamp
     */
    @NotNull(message = "Transaction timestamp cannot be null")
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
     * Transaction type (e.g., PURCHASE, REFUND, TRANSFER)
     */
    private String type;
    
    /**
     * Account ID this transaction belongs to
     */
    private String accountId;
}
