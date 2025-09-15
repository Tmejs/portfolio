package com.portfolio.analytics.repository;

import com.portfolio.analytics.model.AccountAnalytics;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountAnalyticsRepository extends CrudRepository<AccountAnalytics, String> {
    
    Optional<AccountAnalytics> findByAccountId(String accountId);
    
    List<AccountAnalytics> findByLastUpdatedAfter(LocalDateTime after);
    
    List<AccountAnalytics> findBySpendingPattern(String pattern);
    
    List<AccountAnalytics> findByPrimaryCategory(String category);
    
    boolean existsByAccountId(String accountId);
    
    void deleteByAccountId(String accountId);
}
