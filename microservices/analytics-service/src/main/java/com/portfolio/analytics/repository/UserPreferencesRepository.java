package com.portfolio.analytics.repository;

import com.portfolio.analytics.model.UserPreferences;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends CrudRepository<UserPreferences, String> {
    
    Optional<UserPreferences> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
    
    void deleteByUserId(String userId);
}
