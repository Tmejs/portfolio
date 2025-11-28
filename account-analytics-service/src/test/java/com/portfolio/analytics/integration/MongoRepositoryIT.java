package com.portfolio.analytics.integration;

import com.portfolio.analytics.BaseAnalyticsIT;
import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.repository.AccountAnalyticsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests specifically for MongoDB repository functionality.
 * Tests custom queries, indexes, and complex search operations.
 */
@SpringBootTest
class MongoRepositoryIT extends BaseAnalyticsIT {

    @Autowired
    private AccountAnalyticsRepository analyticsRepository;

    @BeforeEach
    void setUp() {
        // Ensure clean state for each test
        analyticsRepository.deleteAll();
        
        // Verify cleanup is complete
        assertThat(analyticsRepository.count()).isEqualTo(0);
    }
    
    @AfterEach
    void tearDown() {
        // Additional cleanup after each test
        analyticsRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveAnalytics() {
        // Given
        String accountId = "test-account-123";
        Map<String, BigDecimal> dailyBalances = new HashMap<>();
        dailyBalances.put("2024-01-01", new BigDecimal("1000.00"));
        dailyBalances.put("2024-01-02", new BigDecimal("1200.00"));

        AccountAnalytics analytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(new BigDecimal("1200.00"))
            .totalIncome(new BigDecimal("2000.00"))
            .totalExpenses(new BigDecimal("800.00"))
            .transactionCount(15L)
            .depositCount(8L)
            .withdrawalCount(7L)
            .spendingPattern("MODERATE")
            .primaryCategory("GROCERIES")
            .volatilityScore(new BigDecimal("2.5"))
            .dailyBalances(dailyBalances)
            .monthlyExpenses(Map.of("2024-01", new BigDecimal("800.00")))
            .monthlyIncome(Map.of("2024-01", new BigDecimal("2000.00")))
            .lastUpdated(LocalDateTime.now())
            .calculatedAt(LocalDateTime.now())
            .build();

        // When
        AccountAnalytics saved = analyticsRepository.save(analytics);

        // Then
        assertThat(saved.getAccountId()).isEqualTo(accountId);
        
        Optional<AccountAnalytics> retrieved = analyticsRepository.findByAccountId(accountId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTotalBalance()).isEqualTo(new BigDecimal("1200.00"));
        assertThat(retrieved.get().getSpendingPattern()).isEqualTo("MODERATE");
        assertThat(retrieved.get().getDailyBalances()).hasSize(2);
    }

    @Test
    void shouldFindBySpendingPattern() {
        // Given
        saveTestAnalytics("account-1", "CONSERVATIVE", new BigDecimal("5000.00"));
        saveTestAnalytics("account-2", "MODERATE", new BigDecimal("3000.00"));
        saveTestAnalytics("account-3", "CONSERVATIVE", new BigDecimal("8000.00"));
        saveTestAnalytics("account-4", "AGGRESSIVE", new BigDecimal("1000.00"));

        // When
        List<AccountAnalytics> conservatives = analyticsRepository.findBySpendingPattern("CONSERVATIVE");
        List<AccountAnalytics> moderates = analyticsRepository.findBySpendingPattern("MODERATE");
        List<AccountAnalytics> aggressives = analyticsRepository.findBySpendingPattern("AGGRESSIVE");

        // Then
        assertThat(conservatives).hasSize(2);
        assertThat(moderates).hasSize(1);
        assertThat(aggressives).hasSize(1);
        assertThat(conservatives.stream().allMatch(a -> a.getSpendingPattern().equals("CONSERVATIVE"))).isTrue();
    }

    @Test
    void shouldFindByPrimaryCategory() {
        // Given
        saveTestAnalytics("account-1", "MODERATE", new BigDecimal("2000.00"), "GROCERIES");
        saveTestAnalytics("account-2", "CONSERVATIVE", new BigDecimal("5000.00"), "SALARY");
        saveTestAnalytics("account-3", "MODERATE", new BigDecimal("3000.00"), "GROCERIES");

        // When
        List<AccountAnalytics> groceries = analyticsRepository.findByPrimaryCategory("GROCERIES");
        List<AccountAnalytics> salary = analyticsRepository.findByPrimaryCategory("SALARY");

        // Then
        assertThat(groceries).hasSize(2);
        assertThat(salary).hasSize(1);
        assertThat(groceries.stream().allMatch(a -> a.getPrimaryCategory().equals("GROCERIES"))).isTrue();
    }

    @Test
    void shouldFindByBalanceRange() {
        // Given - ensure clean state
        assertThat(analyticsRepository.count()).isEqualTo(0);
        
        saveTestAnalytics("low-balance", "CONSERVATIVE", new BigDecimal("500.00"));
        saveTestAnalytics("medium-balance", "MODERATE", new BigDecimal("2500.00"));
        saveTestAnalytics("high-balance", "AGGRESSIVE", new BigDecimal("7500.00"));
        
        // Verify we have exactly 3 records
        assertThat(analyticsRepository.count()).isEqualTo(3);

        // When
        List<AccountAnalytics> highBalance = analyticsRepository.findByTotalBalanceGreaterThanEqual(new BigDecimal("5000.00"));
        List<AccountAnalytics> mediumPlus = analyticsRepository.findByTotalBalanceGreaterThanEqual(new BigDecimal("2000.00"));

        // Then
        assertThat(highBalance).hasSize(1);
        assertThat(highBalance.get(0).getAccountId()).isEqualTo("high-balance");
        
        assertThat(mediumPlus).hasSize(2);
        assertThat(mediumPlus.stream().allMatch(a -> a.getTotalBalance().compareTo(new BigDecimal("2000.00")) >= 0)).isTrue();
    }

    @Test
    void shouldFindByVolatilityScoreRange() {
        // Given
        saveTestAnalyticsWithVolatility("stable-account", new BigDecimal("1.0"));
        saveTestAnalyticsWithVolatility("moderate-account", new BigDecimal("3.5"));
        saveTestAnalyticsWithVolatility("volatile-account", new BigDecimal("8.0"));

        // When
        List<AccountAnalytics> moderateVolatility = analyticsRepository.findByVolatilityScoreBetween(
            new BigDecimal("2.0"), new BigDecimal("5.0"));

        // Then
        assertThat(moderateVolatility).hasSize(1);
        assertThat(moderateVolatility.get(0).getAccountId()).isEqualTo("moderate-account");
        assertThat(moderateVolatility.get(0).getVolatilityScore()).isEqualTo(new BigDecimal("3.5"));
    }

    @Test
    void shouldFindByTransactionCount() {
        // Given
        saveTestAnalyticsWithTransactionCount("low-activity", 5L);
        saveTestAnalyticsWithTransactionCount("medium-activity", 25L);
        saveTestAnalyticsWithTransactionCount("high-activity", 150L);

        // When
        List<AccountAnalytics> activeAccounts = analyticsRepository.findByTransactionCountGreaterThan(20L);

        // Then
        assertThat(activeAccounts).hasSize(2);
        assertThat(activeAccounts.stream().allMatch(a -> a.getTransactionCount() > 20L)).isTrue();
    }

    @Test
    void shouldFindRecentAnalytics() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime oneDayAgo = now.minusDays(1);

        // Create timestamps that are clearly after oneHourAgo
        saveTestAnalyticsWithTimestamp("recent-1", now.minusMinutes(30));  // 30 minutes ago (should be found)
        saveTestAnalyticsWithTimestamp("recent-2", now.minusMinutes(45));  // 45 minutes ago (should be found)
        saveTestAnalyticsWithTimestamp("old-1", oneDayAgo);                // 1 day ago (should not be found)

        // When
        List<AccountAnalytics> recentAnalytics = analyticsRepository.findRecentAnalytics(oneHourAgo);

        // Then
        assertThat(recentAnalytics).hasSize(2);
        assertThat(recentAnalytics.stream().allMatch(a -> a.getLastUpdated().isAfter(oneHourAgo))).isTrue();
    }

    @Test
    void shouldHandleComplexQueries() {
        // Given - Ensure clean state
        assertThat(analyticsRepository.count()).isEqualTo(0);
        
        // Create accounts with different characteristics
        saveComplexTestAnalytics("conservative-high", "CONSERVATIVE", new BigDecimal("10000.00"), 50L, new BigDecimal("1.2"));
        saveComplexTestAnalytics("moderate-medium", "MODERATE", new BigDecimal("5000.00"), 100L, new BigDecimal("3.5"));
        saveComplexTestAnalytics("aggressive-low", "AGGRESSIVE", new BigDecimal("2000.00"), 200L, new BigDecimal("7.8"));
        
        // Verify data was saved
        assertThat(analyticsRepository.count()).isEqualTo(3);

        // When - Test individual queries separately
        List<AccountAnalytics> allAnalytics = analyticsRepository.findAll();
        List<AccountAnalytics> conservativeHigh = analyticsRepository.findBySpendingPattern("CONSERVATIVE");
        List<AccountAnalytics> highBalance = analyticsRepository.findByTotalBalanceGreaterThanEqual(new BigDecimal("8000.00"));
        
        // Debug: Print all accounts to see what was actually saved
        System.out.println("All saved accounts:");
        allAnalytics.forEach(a -> System.out.println("  - " + a.getAccountId() + ": balance=" + a.getTotalBalance() + ", pattern=" + a.getSpendingPattern()));

        // Then - Verify individual queries work correctly
        assertThat(allAnalytics).hasSize(3);  // Ensure all test data was saved
        assertThat(conservativeHigh).hasSize(1);
        assertThat(conservativeHigh.get(0).getAccountId()).isEqualTo("conservative-high");
        assertThat(conservativeHigh.get(0).getSpendingPattern()).isEqualTo("CONSERVATIVE");
        
        assertThat(highBalance).hasSize(1);
        assertThat(highBalance.get(0).getAccountId()).isEqualTo("conservative-high");
        assertThat(highBalance.get(0).getTotalBalance().compareTo(new BigDecimal("8000.00")) >= 0).isTrue();
    }

    @Test
    void shouldHandleExistenceAndDeletion() {
        // Given
        String accountId = "existence-test";
        saveTestAnalytics(accountId, "MODERATE", new BigDecimal("1000.00"));

        // When - Check existence
        boolean existsBefore = analyticsRepository.existsByAccountId(accountId);
        
        // Then
        assertThat(existsBefore).isTrue();

        // When - Delete
        analyticsRepository.deleteByAccountId(accountId);
        boolean existsAfter = analyticsRepository.existsByAccountId(accountId);

        // Then
        assertThat(existsAfter).isFalse();
    }

    // Helper methods
    private void saveTestAnalytics(String accountId, String spendingPattern, BigDecimal balance) {
        saveTestAnalytics(accountId, spendingPattern, balance, "GENERAL");
    }

    private void saveTestAnalytics(String accountId, String spendingPattern, BigDecimal balance, String category) {
        AccountAnalytics analytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(balance)
            .totalIncome(balance.multiply(new BigDecimal("1.5")))
            .totalExpenses(balance.multiply(new BigDecimal("0.5")))
            .transactionCount(10L)
            .spendingPattern(spendingPattern)
            .primaryCategory(category)
            .volatilityScore(new BigDecimal("2.0"))
            .dailyBalances(new HashMap<>())
            .monthlyExpenses(new HashMap<>())
            .monthlyIncome(new HashMap<>())
            .lastUpdated(LocalDateTime.now())
            .calculatedAt(LocalDateTime.now())
            .build();
        
        analyticsRepository.save(analytics);
    }

    private void saveTestAnalyticsWithVolatility(String accountId, BigDecimal volatilityScore) {
        AccountAnalytics analytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(new BigDecimal("1000.00"))
            .spendingPattern("MODERATE")
            .primaryCategory("GENERAL")
            .volatilityScore(volatilityScore)
            .transactionCount(10L)
            .dailyBalances(new HashMap<>())
            .monthlyExpenses(new HashMap<>())
            .monthlyIncome(new HashMap<>())
            .lastUpdated(LocalDateTime.now())
            .calculatedAt(LocalDateTime.now())
            .build();
        
        analyticsRepository.save(analytics);
    }

    private void saveTestAnalyticsWithTransactionCount(String accountId, Long transactionCount) {
        AccountAnalytics analytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(new BigDecimal("1000.00"))
            .spendingPattern("MODERATE")
            .primaryCategory("GENERAL")
            .volatilityScore(new BigDecimal("2.0"))
            .transactionCount(transactionCount)
            .dailyBalances(new HashMap<>())
            .monthlyExpenses(new HashMap<>())
            .monthlyIncome(new HashMap<>())
            .lastUpdated(LocalDateTime.now())
            .calculatedAt(LocalDateTime.now())
            .build();
        
        analyticsRepository.save(analytics);
    }

    private void saveTestAnalyticsWithTimestamp(String accountId, LocalDateTime lastUpdated) {
        AccountAnalytics analytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(new BigDecimal("1000.00"))
            .spendingPattern("MODERATE")
            .primaryCategory("GENERAL")
            .volatilityScore(new BigDecimal("2.0"))
            .transactionCount(10L)
            .dailyBalances(new HashMap<>())
            .monthlyExpenses(new HashMap<>())
            .monthlyIncome(new HashMap<>())
            .lastUpdated(lastUpdated)
            .calculatedAt(lastUpdated)
            .build();
        
        analyticsRepository.save(analytics);
    }

    private void saveComplexTestAnalytics(String accountId, String pattern, BigDecimal balance, Long txCount, BigDecimal volatility) {
        AccountAnalytics analytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(balance)
            .totalIncome(balance.multiply(new BigDecimal("1.3")))
            .totalExpenses(balance.multiply(new BigDecimal("0.3")))
            .transactionCount(txCount)
            .spendingPattern(pattern)
            .primaryCategory("MIXED")
            .volatilityScore(volatility)
            .dailyBalances(new HashMap<>())
            .monthlyExpenses(new HashMap<>())
            .monthlyIncome(new HashMap<>())
            .lastUpdated(LocalDateTime.now())
            .calculatedAt(LocalDateTime.now())
            .build();
        
        analyticsRepository.save(analytics);
    }
}
