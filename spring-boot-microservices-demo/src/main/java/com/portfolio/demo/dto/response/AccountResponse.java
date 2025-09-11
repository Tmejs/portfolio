package com.portfolio.demo.dto.response;

import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for account information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    
    private Long id;
    private String accountNumber;
    private String customerName;
    private String customerEmail;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
