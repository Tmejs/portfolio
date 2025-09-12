package com.portfolio.demo.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message DTO for account creation events
 * Used for JMS messaging when an account is created
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedMessage {
    
    private Long accountId;
    private String accountNumber;
    private String customerName;
    private String customerEmail;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Event metadata
    @Builder.Default
    private String eventType = "ACCOUNT_CREATED";
    @Builder.Default
    private String eventVersion = "1.0";
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTimestamp;
}
