package com.portfolio.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("account_analytics")
public class AccountAnalytics implements Serializable {
    
    @Id
    private String accountId;
    
    private BigDecimal totalBalance;
    
    private BigDecimal totalIncome;
    
    private BigDecimal totalExpenses;
    
    private Long transactionCount;
    
    private Long depositCount;
    
    private Long withdrawalCount;
    
    private BigDecimal averageTransactionAmount;
    
    private BigDecimal largestDeposit;
    
    private BigDecimal largestWithdrawal;
    
    private LocalDateTime lastTransactionDate;
    
    private LocalDateTime firstTransactionDate;
    
    // Daily/Monthly/Yearly breakdowns
    private Map<String, BigDecimal> dailyBalances;
    
    private Map<String, BigDecimal> monthlyExpenses;
    
    private Map<String, BigDecimal> monthlyIncome;
    
    // Risk and behavior metrics
    private BigDecimal volatilityScore;
    
    private String spendingPattern; // "conservative", "moderate", "aggressive"
    
    private String primaryCategory; // based on transaction categories
    
    // Cache metadata
    private LocalDateTime lastUpdated;
    
    private LocalDateTime calculatedAt;
}
