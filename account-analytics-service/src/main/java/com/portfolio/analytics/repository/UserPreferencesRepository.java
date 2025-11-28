package com.portfolio.analytics.repository;

import com.portfolio.analytics.model.UserPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for UserPreferences entities with MongoDB persistence.
 * Supports user preferences storage and retrieval with flexible querying.
 */
@Repository
public interface UserPreferencesRepository extends MongoRepository<UserPreferences, String> {
    
    /**
     * Find user preferences by user ID
     */
    Optional<UserPreferences> findByUserId(String userId);
    
    /**
     * Check if preferences exist for a user
     */
    boolean existsByUserId(String userId);
    
    /**
     * Delete preferences by user ID
     */
    void deleteByUserId(String userId);
    
    /**
     * Find preferences by theme
     */
    List<UserPreferences> findByTheme(String theme);
    
    /**
     * Find preferences by language
     */
    List<UserPreferences> findByLanguage(String language);
    
    /**
     * Find preferences by currency
     */
    List<UserPreferences> findByCurrency(String currency);
    
    /**
     * Find preferences with notifications enabled
     */
    List<UserPreferences> findByNotificationsEnabled(boolean enabled);
    
    /**
     * Find preferences updated after a specific date
     */
    List<UserPreferences> findByLastUpdatedAfter(LocalDateTime after);
    
    /**
     * Find recent preferences (updated within last N days)
     */
    @Query("{ 'lastUpdated' : { $gte: ?0 } }")
    List<UserPreferences> findRecentPreferences(LocalDateTime since);
}
