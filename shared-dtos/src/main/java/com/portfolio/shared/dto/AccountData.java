package com.portfolio.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for account information shared across Portfolio microservices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountData {
    
    /**
     * Unique identifier for the account
     */
    @NotBlank(message = "Account ID cannot be blank")
    private String accountId;
    
    /**
     * Account holder's user ID
     */
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    /**
     * Account number (masked or full depending on context)
     */
    private String accountNumber;
    
    /**
     * Account type (e.g., CHECKING, SAVINGS, CREDIT)
     */
    @NotBlank(message = "Account type cannot be blank")
    private String accountType;
    
    /**
     * Current account balance
     */
    @NotNull(message = "Balance cannot be null")
    private BigDecimal balance;
    
    /**
     * Available balance (may differ from balance due to holds)
     */
    private BigDecimal availableBalance;
    
    /**
     * Currency code
     */
    @Builder.Default
    private String currency = "USD";
    
    /**
     * Account status (e.g., ACTIVE, CLOSED, FROZEN)
     */
    @Builder.Default
    private String status = "ACTIVE";
    
    /**
     * Account name or nickname
     */
    private String accountName;
    
    /**
     * Date when the account was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Date when the account was last updated
     */
    private LocalDateTime lastUpdated;
}
