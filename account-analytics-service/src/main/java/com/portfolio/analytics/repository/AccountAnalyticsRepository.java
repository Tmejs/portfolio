package com.portfolio.analytics.repository;

import com.portfolio.analytics.model.AccountAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AccountAnalytics entities with MongoDB persistence.
 * Supports advanced queries for analytics data retrieval and aggregation.
 */
@Repository
public interface AccountAnalyticsRepository extends MongoRepository<AccountAnalytics, String> {
    
    /**
     * Find analytics by account ID
     */
    Optional<AccountAnalytics> findByAccountId(String accountId);
    
    /**
     * Find analytics updated after a specific date
     */
    List<AccountAnalytics> findByLastUpdatedAfter(LocalDateTime after);
    
    /**
     * Find analytics by spending pattern (CONSERVATIVE, MODERATE, AGGRESSIVE, INACTIVE)
     */
    List<AccountAnalytics> findBySpendingPattern(String pattern);
    
    /**
     * Find analytics by primary transaction category
     */
    List<AccountAnalytics> findByPrimaryCategory(String category);
    
    /**
     * Check if analytics exist for an account
     */
    boolean existsByAccountId(String accountId);
    
    /**
     * Delete analytics by account ID
     */
    void deleteByAccountId(String accountId);
    
    /**
     * Find analytics with total balance greater than specified amount
     */
    @Query("{ 'totalBalance' : { $gte: ?0 } }")
    List<AccountAnalytics> findByTotalBalanceGreaterThanEqual(BigDecimal minBalance);
    
    /**
     * Find analytics by volatility score range
     */
    @Query("{ 'volatilityScore' : { $gte: ?0, $lte: ?1 } }")
    List<AccountAnalytics> findByVolatilityScoreBetween(BigDecimal minScore, BigDecimal maxScore);
    
    /**
     * Find analytics with transaction count greater than specified value
     */
    List<AccountAnalytics> findByTransactionCountGreaterThan(Long minCount);
    
    /**
     * Find recent analytics (updated within last N days)
     */
    @Query("{ 'lastUpdated' : { $gte: ?0 } }")
    List<AccountAnalytics> findRecentAnalytics(LocalDateTime since);
}
