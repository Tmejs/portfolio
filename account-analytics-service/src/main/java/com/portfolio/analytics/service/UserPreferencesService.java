package com.portfolio.analytics.service;

import com.portfolio.analytics.model.UserPreferences;
import com.portfolio.analytics.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;

    @Cacheable(value = "user-preferences", key = "#userId")
    public Optional<UserPreferences> getUserPreferences(String userId) {
        log.info("Fetching user preferences for user: {}", userId);
        return userPreferencesRepository.findByUserId(userId);
    }

    @CachePut(value = "user-preferences", key = "#userPreferences.userId")
    public UserPreferences saveUserPreferences(UserPreferences userPreferences) {
        log.info("Saving user preferences for user: {}", userPreferences.getUserId());
        
        if (userPreferences.getCreatedAt() == null) {
            userPreferences.setCreatedAt(LocalDateTime.now());
        }
        userPreferences.setLastUpdated(LocalDateTime.now());
        
        return userPreferencesRepository.save(userPreferences);
    }

    @CachePut(value = "user-preferences", key = "#userId")
    public UserPreferences updateUserPreferences(String userId, UserPreferences updates) {
        log.info("Updating user preferences for user: {}", userId);
        
        UserPreferences existing = getUserPreferences(userId)
            .orElse(UserPreferences.builder()
                .userId(userId)
                .theme("light")
                .language("en")
                .currency("USD")
                .timezone("UTC")
                .notificationsEnabled(true)
                .customSettings(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .build());

        // Update fields if provided
        if (updates.getTheme() != null) existing.setTheme(updates.getTheme());
        if (updates.getLanguage() != null) existing.setLanguage(updates.getLanguage());
        if (updates.getCurrency() != null) existing.setCurrency(updates.getCurrency());
        if (updates.getTimezone() != null) existing.setTimezone(updates.getTimezone());
        if (updates.getCustomSettings() != null) existing.setCustomSettings(updates.getCustomSettings());
        
        existing.setNotificationsEnabled(updates.isNotificationsEnabled());
        existing.setLastUpdated(LocalDateTime.now());

        return userPreferencesRepository.save(existing);
    }

    @CacheEvict(value = "user-preferences", key = "#userId")
    @Transactional
    public void deleteUserPreferences(String userId) {
        log.info("Deleting user preferences for user: {}", userId);
        userPreferencesRepository.deleteByUserId(userId);
    }

    public boolean existsUserPreferences(String userId) {
        return userPreferencesRepository.existsByUserId(userId);
    }
}
