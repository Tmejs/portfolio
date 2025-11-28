package com.portfolio.analytics.service;

import com.portfolio.shared.dto.TransactionData;
import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.repository.AccountAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountAnalyticsService {

    private final AccountAnalyticsRepository accountAnalyticsRepository;

    @Cacheable(value = "analytics", key = "#accountId")
    public Optional<AccountAnalytics> getAccountAnalytics(String accountId) {
        log.info("Fetching analytics for account: {}", accountId);
        return accountAnalyticsRepository.findByAccountId(accountId);
    }

    @CachePut(value = "analytics", key = "#analytics.accountId")
    public AccountAnalytics saveAccountAnalytics(AccountAnalytics analytics) {
        log.info("Saving analytics for account: {}", analytics.getAccountId());
        analytics.setLastUpdated(LocalDateTime.now());
        return accountAnalyticsRepository.save(analytics);
    }

    @CacheEvict(value = "analytics", key = "#accountId")
    public void invalidateAnalytics(String accountId) {
        log.info("Invalidating analytics cache for account: {}", accountId);
    }

    @CacheEvict(value = "analytics", allEntries = true)
    public void invalidateAllAnalytics() {
        log.info("Invalidating all analytics cache");
    }

    // Compute analytics from raw transaction data
    public AccountAnalytics computeAnalytics(String accountId, List<TransactionData> transactions) {
        log.info("Computing analytics for account: {} with {} transactions", accountId, transactions.size());

        if (transactions.isEmpty()) {
            return createEmptyAnalytics(accountId);
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalBalance = BigDecimal.ZERO;
        long depositCount = 0;
        long withdrawalCount = 0;
        BigDecimal largestDeposit = BigDecimal.ZERO;
        BigDecimal largestWithdrawal = BigDecimal.ZERO;
        LocalDateTime firstTransaction = null;
        LocalDateTime lastTransaction = null;
        
        Map<String, BigDecimal> dailyBalances = new HashMap<>();
        Map<String, BigDecimal> monthlyExpenses = new HashMap<>();
        Map<String, BigDecimal> monthlyIncome = new HashMap<>();
        Map<String, Integer> categoryCount = new HashMap<>();

        for (TransactionData transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            LocalDateTime date = transaction.getTransactionDate();
            String category = transaction.getCategory();
            
            // Track balance
            totalBalance = totalBalance.add(amount);
            
            // Categorize income vs expenses
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                totalIncome = totalIncome.add(amount);
                depositCount++;
                if (amount.compareTo(largestDeposit) > 0) {
                    largestDeposit = amount;
                }
                
                // Monthly income tracking
                String monthKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthlyIncome.merge(monthKey, amount, BigDecimal::add);
            } else {
                BigDecimal expenseAmount = amount.abs();
                totalExpenses = totalExpenses.add(expenseAmount);
                withdrawalCount++;
                if (expenseAmount.compareTo(largestWithdrawal) > 0) {
                    largestWithdrawal = expenseAmount;
                }
                
                // Monthly expense tracking
                String monthKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthlyExpenses.merge(monthKey, expenseAmount, BigDecimal::add);
            }
            
            // Daily balance tracking
            String dayKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dailyBalances.put(dayKey, totalBalance);
            
            // Category analysis
            if (category != null) {
                categoryCount.merge(category, 1, Integer::sum);
            }
            
            // Date range tracking
            if (firstTransaction == null || date.isBefore(firstTransaction)) {
                firstTransaction = date;
            }
            if (lastTransaction == null || date.isAfter(lastTransaction)) {
                lastTransaction = date;
            }
        }

        // Calculate derived metrics
        BigDecimal averageTransaction = totalBalance.divide(
            BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
        
        String primaryCategory = categoryCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN");
        
        String spendingPattern = determineSpendingPattern(totalIncome, totalExpenses, transactions.size());
        
        BigDecimal volatilityScore = calculateVolatilityScore(transactions);

        return AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(totalBalance)
            .totalIncome(totalIncome)
            .totalExpenses(totalExpenses)
            .transactionCount((long) transactions.size())
            .depositCount(depositCount)
            .withdrawalCount(withdrawalCount)
            .averageTransactionAmount(averageTransaction)
            .largestDeposit(largestDeposit)
            .largestWithdrawal(largestWithdrawal)
            .lastTransactionDate(lastTransaction)
            .firstTransactionDate(firstTransaction)
            .dailyBalances(dailyBalances)
            .monthlyExpenses(monthlyExpenses)
            .monthlyIncome(monthlyIncome)
            .volatilityScore(volatilityScore)
            .spendingPattern(spendingPattern)
            .primaryCategory(primaryCategory)
            .calculatedAt(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    public List<AccountAnalytics> getAnalyticsBySpendingPattern(String pattern) {
        log.info("Fetching analytics by spending pattern: {}", pattern);
        return accountAnalyticsRepository.findBySpendingPattern(pattern);
    }

    public List<AccountAnalytics> getAnalyticsByCategory(String category) {
        log.info("Fetching analytics by primary category: {}", category);
        return accountAnalyticsRepository.findByPrimaryCategory(category);
    }

    public List<AccountAnalytics> getRecentlyUpdatedAnalytics(LocalDateTime since) {
        log.info("Fetching analytics updated since: {}", since);
        return accountAnalyticsRepository.findByLastUpdatedAfter(since);
    }

    public boolean analyticsExist(String accountId) {
        return accountAnalyticsRepository.existsByAccountId(accountId);
    }

    @CacheEvict(value = "analytics", key = "#accountId")
    public void deleteAnalytics(String accountId) {
        log.info("Deleting analytics for account: {}", accountId);
        accountAnalyticsRepository.deleteByAccountId(accountId);
    }

    // Warm cache with fresh analytics
    public void warmCache(String accountId, List<TransactionData> transactions) {
        log.info("Warming cache for account: {}", accountId);
        AccountAnalytics analytics = computeAnalytics(accountId, transactions);
        saveAccountAnalytics(analytics);
    }

    private AccountAnalytics createEmptyAnalytics(String accountId) {
        return AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(BigDecimal.ZERO)
            .totalIncome(BigDecimal.ZERO)
            .totalExpenses(BigDecimal.ZERO)
            .transactionCount(0L)
            .depositCount(0L)
            .withdrawalCount(0L)
            .averageTransactionAmount(BigDecimal.ZERO)
            .largestDeposit(BigDecimal.ZERO)
            .largestWithdrawal(BigDecimal.ZERO)
            .dailyBalances(new HashMap<>())
            .monthlyExpenses(new HashMap<>())
            .monthlyIncome(new HashMap<>())
            .volatilityScore(BigDecimal.ZERO)
            .spendingPattern("INACTIVE")
            .primaryCategory("NONE")
            .calculatedAt(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    private String determineSpendingPattern(BigDecimal income, BigDecimal expenses, int transactionCount) {
        if (transactionCount < 3) return "INACTIVE";
        
        if (income.compareTo(BigDecimal.ZERO) == 0) return "EXPENSE_ONLY";
        
        BigDecimal expenseRatio = expenses.divide(income, 2, RoundingMode.HALF_UP);
        
        if (expenseRatio.compareTo(BigDecimal.valueOf(0.3)) <= 0) return "CONSERVATIVE";
        if (expenseRatio.compareTo(BigDecimal.valueOf(0.7)) <= 0) return "MODERATE";
        return "AGGRESSIVE";
    }

    private BigDecimal calculateVolatilityScore(List<TransactionData> transactions) {
        if (transactions.size() < 2) return BigDecimal.ZERO;
        
        BigDecimal mean = transactions.stream()
            .map(TransactionData::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal variance = transactions.stream()
            .map(t -> t.getAmount().subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
            .setScale(2, RoundingMode.HALF_UP);
    }

}
