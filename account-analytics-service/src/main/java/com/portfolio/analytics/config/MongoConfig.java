package com.portfolio.analytics.config;

import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.model.UserPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import jakarta.annotation.PostConstruct;

/**
 * MongoDB configuration class for setting up database indexes and performance optimizations.
 * This class creates additional indexes that complement the annotations on model classes.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    /**
     * Set up MongoDB indexes after the application context is fully initialized.
     * This method creates performance-critical indexes that enhance query speed.
     */
    @PostConstruct
    public void setupIndexes() {
        log.info("Setting up MongoDB indexes for analytics collections...");
        
        setupAccountAnalyticsIndexes();
        setupUserPreferencesIndexes();
        
        log.info("MongoDB indexes setup completed successfully");
    }

    /**
     * Create indexes for AccountAnalytics collection
     */
    private void setupAccountAnalyticsIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(AccountAnalytics.class);

        // Performance indexes for common queries
        indexOps.ensureIndex(new Index()
            .on("totalBalance", Sort.Direction.DESC)
            .named("total_balance_desc_idx"));

        indexOps.ensureIndex(new Index()
            .on("volatilityScore", Sort.Direction.ASC)
            .named("volatility_score_asc_idx"));

        indexOps.ensureIndex(new Index()
            .on("transactionCount", Sort.Direction.DESC)
            .named("transaction_count_desc_idx"));

        // Compound index for analytics queries by pattern and balance
        indexOps.ensureIndex(new Index()
            .on("spendingPattern", Sort.Direction.ASC)
            .on("totalBalance", Sort.Direction.DESC)
            .named("pattern_balance_idx"));

        // Time-based queries index
        indexOps.ensureIndex(new Index()
            .on("lastTransactionDate", Sort.Direction.DESC)
            .named("last_transaction_desc_idx"));

        // Sparse index for volatile accounts only
        indexOps.ensureIndex(new Index()
            .on("volatilityScore", Sort.Direction.DESC)
            .sparse()
            .named("high_volatility_sparse_idx"));

        log.info("AccountAnalytics indexes created successfully");
    }

    /**
     * Create indexes for UserPreferences collection
     */
    private void setupUserPreferencesIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(UserPreferences.class);

        // Performance indexes for user queries
        indexOps.ensureIndex(new Index()
            .on("currency", Sort.Direction.ASC)
            .named("currency_asc_idx"));

        indexOps.ensureIndex(new Index()
            .on("createdAt", Sort.Direction.DESC)
            .named("created_at_desc_idx"));

        // Compound index for preferences analytics
        indexOps.ensureIndex(new Index()
            .on("currency", Sort.Direction.ASC)
            .on("theme", Sort.Direction.ASC)
            .named("currency_theme_idx"));

        // Index for notification queries
        indexOps.ensureIndex(new Index()
            .on("notificationsEnabled", Sort.Direction.ASC)
            .on("lastUpdated", Sort.Direction.DESC)
            .named("notifications_updated_idx"));

        log.info("UserPreferences indexes created successfully");
    }
}
