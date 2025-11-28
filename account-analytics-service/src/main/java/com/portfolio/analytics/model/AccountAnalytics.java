package com.portfolio.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "account_analytics")
@CompoundIndex(name = "account_updated_idx", def = "{'accountId': 1, 'lastUpdated': -1}")
@CompoundIndex(name = "pattern_category_idx", def = "{'spendingPattern': 1, 'primaryCategory': 1}")
public class AccountAnalytics implements Serializable {
    
    /**
     * Unique account identifier - serves as both MongoDB _id and Redis key
     */
    @Id
    @Field("_id")
    private String accountId;
    
    /**
     * Total current balance for the account
     */
    @Field("total_balance")
    @Indexed
    private BigDecimal totalBalance;
    
    /**
     * Total income (positive transactions)
     */
    @Field("total_income")
    private BigDecimal totalIncome;
    
    /**
     * Total expenses (negative transactions)
     */
    @Field("total_expenses")
    private BigDecimal totalExpenses;
    
    /**
     * Total number of transactions
     */
    @Field("transaction_count")
    @Indexed
    private Long transactionCount;
    
    /**
     * Number of deposit transactions
     */
    @Field("deposit_count")
    private Long depositCount;
    
    /**
     * Number of withdrawal transactions
     */
    @Field("withdrawal_count")
    private Long withdrawalCount;
    
    /**
     * Average transaction amount
     */
    @Field("average_transaction_amount")
    private BigDecimal averageTransactionAmount;
    
    /**
     * Largest single deposit
     */
    @Field("largest_deposit")
    private BigDecimal largestDeposit;
    
    /**
     * Largest single withdrawal
     */
    @Field("largest_withdrawal")
    private BigDecimal largestWithdrawal;
    
    /**
     * Date of the most recent transaction
     */
    @Field("last_transaction_date")
    @Indexed
    private LocalDateTime lastTransactionDate;
    
    /**
     * Date of the first transaction
     */
    @Field("first_transaction_date")
    private LocalDateTime firstTransactionDate;
    
    /**
     * Daily balance snapshots (date -> balance)
     */
    @Field("daily_balances")
    private Map<String, BigDecimal> dailyBalances;
    
    /**
     * Monthly expense totals (YYYY-MM -> amount)
     */
    @Field("monthly_expenses")
    private Map<String, BigDecimal> monthlyExpenses;
    
    /**
     * Monthly income totals (YYYY-MM -> amount)
     */
    @Field("monthly_income")
    private Map<String, BigDecimal> monthlyIncome;
    
    /**
     * Statistical measure of transaction volatility
     */
    @Field("volatility_score")
    @Indexed
    private BigDecimal volatilityScore;
    
    /**
     * Spending behavior pattern (CONSERVATIVE, MODERATE, AGGRESSIVE, INACTIVE)
     */
    @Field("spending_pattern")
    @Indexed
    private String spendingPattern;
    
    /**
     * Primary transaction category based on frequency
     */
    @Field("primary_category")
    @Indexed
    private String primaryCategory;
    
    /**
     * Timestamp when this record was last updated
     */
    @Field("last_updated")
    @Indexed
    private LocalDateTime lastUpdated;
    
    /**
     * Timestamp when analytics were calculated
     */
    @Field("calculated_at")
    private LocalDateTime calculatedAt;
}
