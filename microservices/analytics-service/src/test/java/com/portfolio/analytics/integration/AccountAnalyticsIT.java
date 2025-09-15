package com.portfolio.analytics.integration;

import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.service.AccountAnalyticsService;
import com.portfolio.analytics.service.AccountAnalyticsService.TransactionData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DirtiesContext
class AccountAnalyticsIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // Disable MongoDB and Kafka for this test
        registry.add("spring.data.mongodb.uri", () -> "mongodb://disabled:27017/disabled");
        registry.add("spring.kafka.bootstrap-servers", () -> "disabled");
    }

    @Autowired
    private AccountAnalyticsService analyticsService;

    @Test
    void shouldComputeAndCacheAnalytics() {
        // Given
        String accountId = "test-account-123";
        List<TransactionData> transactions = List.of(
            TransactionData.builder()
                .transactionId("t1")
                .amount(new BigDecimal("1000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.now().minusDays(30))
                .description("Monthly salary")
                .build(),
            TransactionData.builder()
                .transactionId("t2")
                .amount(new BigDecimal("-200.00"))
                .category("GROCERIES")
                .transactionDate(LocalDateTime.now().minusDays(25))
                .description("Grocery shopping")
                .build(),
            TransactionData.builder()
                .transactionId("t3")
                .amount(new BigDecimal("-150.00"))
                .category("UTILITIES")
                .transactionDate(LocalDateTime.now().minusDays(20))
                .description("Electric bill")
                .build(),
            TransactionData.builder()
                .transactionId("t4")
                .amount(new BigDecimal("500.00"))
                .category("FREELANCE")
                .transactionDate(LocalDateTime.now().minusDays(15))
                .description("Freelance payment")
                .build()
        );

        // When
        AccountAnalytics analytics = analyticsService.computeAnalytics(accountId, transactions);
        AccountAnalytics saved = analyticsService.saveAccountAnalytics(analytics);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getAccountId()).isEqualTo(accountId);
        assertThat(saved.getTotalBalance()).isEqualTo(new BigDecimal("1150.00"));
        assertThat(saved.getTotalIncome()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(saved.getTotalExpenses()).isEqualTo(new BigDecimal("350.00"));
        assertThat(saved.getTransactionCount()).isEqualTo(4L);
        assertThat(saved.getDepositCount()).isEqualTo(2L);
        assertThat(saved.getWithdrawalCount()).isEqualTo(2L);
        assertThat(saved.getLargestDeposit()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(saved.getLargestWithdrawal()).isEqualTo(new BigDecimal("200.00"));
        assertThat(saved.getSpendingPattern()).isEqualTo("CONSERVATIVE");
        assertThat(saved.getPrimaryCategory()).isIn("SALARY", "GROCERIES", "UTILITIES", "FREELANCE");
        assertThat(saved.getVolatilityScore()).isNotNull();

        // Verify caching works
        Optional<AccountAnalytics> cached = analyticsService.getAccountAnalytics(accountId);
        assertThat(cached).isPresent();
        assertThat(cached.get().getAccountId()).isEqualTo(accountId);
        assertThat(cached.get().getTotalBalance()).isEqualTo(saved.getTotalBalance());
    }

    @Test
    void shouldHandleEmptyTransactions() {
        // Given
        String accountId = "empty-account-456";
        List<TransactionData> transactions = List.of();

        // When
        AccountAnalytics analytics = analyticsService.computeAnalytics(accountId, transactions);

        // Then
        assertThat(analytics.getAccountId()).isEqualTo(accountId);
        assertThat(analytics.getTotalBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(analytics.getTotalIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(analytics.getTotalExpenses()).isEqualTo(BigDecimal.ZERO);
        assertThat(analytics.getTransactionCount()).isEqualTo(0L);
        assertThat(analytics.getSpendingPattern()).isEqualTo("INACTIVE");
        assertThat(analytics.getPrimaryCategory()).isEqualTo("NONE");
    }

    @Test
    void shouldDetermineSpendingPatterns() {
        // Given - Conservative spender (low expense ratio)
        String conservativeAccountId = "conservative-789";
        List<TransactionData> conservativeTransactions = List.of(
            TransactionData.builder()
                .amount(new BigDecimal("3000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-500.00"))
                .category("RENT")
                .transactionDate(LocalDateTime.now().minusDays(2))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-200.00"))
                .category("GROCERIES")
                .transactionDate(LocalDateTime.now().minusDays(3))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-100.00"))
                .category("UTILITIES")
                .transactionDate(LocalDateTime.now().minusDays(4))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-50.00"))
                .category("MISCELLANEOUS")
                .transactionDate(LocalDateTime.now().minusDays(5))
                .build()
        );

        // Given - Aggressive spender (high expense ratio)
        String aggressiveAccountId = "aggressive-999";
        List<TransactionData> aggressiveTransactions = List.of(
            TransactionData.builder()
                .amount(new BigDecimal("2000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-800.00"))
                .category("RENT")
                .transactionDate(LocalDateTime.now().minusDays(2))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-500.00"))
                .category("SHOPPING")
                .transactionDate(LocalDateTime.now().minusDays(3))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-400.00"))
                .category("ENTERTAINMENT")
                .transactionDate(LocalDateTime.now().minusDays(4))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-600.00"))
                .category("MISCELLANEOUS")
                .transactionDate(LocalDateTime.now().minusDays(5))
                .build()
        );

        // When
        AccountAnalytics conservativeAnalytics = analyticsService.computeAnalytics(conservativeAccountId, conservativeTransactions);
        AccountAnalytics aggressiveAnalytics = analyticsService.computeAnalytics(aggressiveAccountId, aggressiveTransactions);

        // Then
        assertThat(conservativeAnalytics.getSpendingPattern()).isEqualTo("CONSERVATIVE");
        assertThat(aggressiveAnalytics.getSpendingPattern()).isEqualTo("AGGRESSIVE");

        // Verify total calculations
        assertThat(conservativeAnalytics.getTotalIncome()).isEqualTo(new BigDecimal("3000.00"));
        assertThat(conservativeAnalytics.getTotalExpenses()).isEqualTo(new BigDecimal("850.00"));
        
        assertThat(aggressiveAnalytics.getTotalIncome()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(aggressiveAnalytics.getTotalExpenses()).isEqualTo(new BigDecimal("2300.00"));
    }

    @Test
    void shouldCalculateMonthlyBreakdowns() {
        // Given
        String accountId = "monthly-breakdown-111";
        List<TransactionData> transactions = List.of(
            // January transactions
            TransactionData.builder()
                .amount(new BigDecimal("2000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-500.00"))
                .category("RENT")
                .transactionDate(LocalDateTime.of(2024, 1, 20, 10, 0))
                .build(),
            // February transactions
            TransactionData.builder()
                .amount(new BigDecimal("2000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.of(2024, 2, 15, 10, 0))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-600.00"))
                .category("RENT")
                .transactionDate(LocalDateTime.of(2024, 2, 20, 10, 0))
                .build()
        );

        // When
        AccountAnalytics analytics = analyticsService.computeAnalytics(accountId, transactions);

        // Then
        assertThat(analytics.getMonthlyIncome()).containsEntry("2024-01", new BigDecimal("2000.00"));
        assertThat(analytics.getMonthlyIncome()).containsEntry("2024-02", new BigDecimal("2000.00"));
        assertThat(analytics.getMonthlyExpenses()).containsEntry("2024-01", new BigDecimal("500.00"));
        assertThat(analytics.getMonthlyExpenses()).containsEntry("2024-02", new BigDecimal("600.00"));
        assertThat(analytics.getDailyBalances()).hasSize(4); // 4 transaction days
    }

    @Test
    void shouldCacheInvalidation() {
        // Given
        String accountId = "cache-test-222";
        List<TransactionData> initialTransactions = List.of(
            TransactionData.builder()
                .amount(new BigDecimal("1000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build()
        );

        // When - Initial cache
        AccountAnalytics initial = analyticsService.computeAnalytics(accountId, initialTransactions);
        analyticsService.saveAccountAnalytics(initial);

        // Then - Should be cached
        Optional<AccountAnalytics> cached = analyticsService.getAccountAnalytics(accountId);
        assertThat(cached).isPresent();
        assertThat(cached.get().getTotalBalance()).isEqualTo(new BigDecimal("1000.00"));

        // When - Invalidate cache
        analyticsService.invalidateAnalytics(accountId);

        // Add more transactions and recompute
        List<TransactionData> updatedTransactions = List.of(
            TransactionData.builder()
                .amount(new BigDecimal("1000.00"))
                .category("SALARY")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build(),
            TransactionData.builder()
                .amount(new BigDecimal("-200.00"))
                .category("GROCERIES")
                .transactionDate(LocalDateTime.now())
                .build()
        );

        AccountAnalytics updated = analyticsService.computeAnalytics(accountId, updatedTransactions);
        analyticsService.saveAccountAnalytics(updated);

        // Then - Should have updated values
        Optional<AccountAnalytics> newCached = analyticsService.getAccountAnalytics(accountId);
        assertThat(newCached).isPresent();
        assertThat(newCached.get().getTotalBalance()).isEqualTo(new BigDecimal("800.00"));
        assertThat(newCached.get().getTransactionCount()).isEqualTo(2L);
    }

    @Test
    void shouldWarmCache() {
        // Given
        String accountId = "warm-cache-333";
        List<TransactionData> transactions = List.of(
            TransactionData.builder()
                .amount(new BigDecimal("500.00"))
                .category("BONUS")
                .transactionDate(LocalDateTime.now())
                .build()
        );

        // When
        analyticsService.warmCache(accountId, transactions);

        // Then
        Optional<AccountAnalytics> analytics = analyticsService.getAccountAnalytics(accountId);
        assertThat(analytics).isPresent();
        assertThat(analytics.get().getTotalBalance()).isEqualTo(new BigDecimal("500.00"));
        assertThat(analytics.get().getPrimaryCategory()).isEqualTo("BONUS");
    }

    @Test
    void shouldHandleAnalyticsExistenceAndDeletion() {
        // Given
        String accountId = "exist-delete-444";
        List<TransactionData> transactions = List.of(
            TransactionData.builder()
                .amount(new BigDecimal("100.00"))
                .category("TEST")
                .transactionDate(LocalDateTime.now())
                .build()
        );

        // When - Create analytics
        AccountAnalytics analytics = analyticsService.computeAnalytics(accountId, transactions);
        analyticsService.saveAccountAnalytics(analytics);

        // Then - Should exist
        assertThat(analyticsService.analyticsExist(accountId)).isTrue();
        assertThat(analyticsService.getAccountAnalytics(accountId)).isPresent();

        // When - Delete analytics
        analyticsService.deleteAnalytics(accountId);

        // Then - Should not exist
        assertThat(analyticsService.analyticsExist(accountId)).isFalse();
        assertThat(analyticsService.getAccountAnalytics(accountId)).isEmpty();
    }
}
