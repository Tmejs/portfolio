package com.portfolio.analytics.integration;

import com.portfolio.analytics.model.UserPreferences;
import com.portfolio.analytics.service.UserPreferencesService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DirtiesContext
class RedisIT {

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
    private UserPreferencesService userPreferencesService;

    @Test
    void shouldSaveAndRetrieveUserPreferences() {
        // Given
        String userId = "test-user-123";
        Map<String, Object> customSettings = new HashMap<>();
        customSettings.put("dashboardLayout", "compact");
        customSettings.put("autoRefresh", true);

        UserPreferences preferences = UserPreferences.builder()
            .userId(userId)
            .theme("dark")
            .language("en")
            .currency("USD")
            .timezone("UTC")
            .notificationsEnabled(true)
            .customSettings(customSettings)
            .createdAt(LocalDateTime.now())
            .build();

        // When
        UserPreferences saved = userPreferencesService.saveUserPreferences(preferences);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTheme()).isEqualTo("dark");
        assertThat(saved.getCustomSettings()).containsKey("dashboardLayout");

        // Verify retrieval from cache
        Optional<UserPreferences> retrieved = userPreferencesService.getUserPreferences(userId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUserId()).isEqualTo(userId);
        assertThat(retrieved.get().getTheme()).isEqualTo("dark");
    }

    @Test
    void shouldUpdateUserPreferences() {
        // Given
        String userId = "test-user-update-456";
        UserPreferences initial = UserPreferences.builder()
            .userId(userId)
            .theme("light")
            .language("en")
            .currency("USD")
            .timezone("UTC")
            .notificationsEnabled(true)
            .customSettings(new HashMap<>())
            .build();

        userPreferencesService.saveUserPreferences(initial);

        // When
        UserPreferences updates = UserPreferences.builder()
            .theme("dark")
            .language("es")
            .currency("EUR")
            .notificationsEnabled(false)
            .build();

        UserPreferences updated = userPreferencesService.updateUserPreferences(userId, updates);

        // Then
        assertThat(updated.getTheme()).isEqualTo("dark");
        assertThat(updated.getLanguage()).isEqualTo("es");
        assertThat(updated.getCurrency()).isEqualTo("EUR");
        assertThat(updated.isNotificationsEnabled()).isFalse();
        assertThat(updated.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldCacheUserPreferences() {
        // Given
        String userId = "test-user-cache-789";
        UserPreferences preferences = UserPreferences.builder()
            .userId(userId)
            .theme("dark")
            .language("fr")
            .currency("CAD")
            .timezone("America/Toronto")
            .notificationsEnabled(true)
            .customSettings(new HashMap<>())
            .build();

        // When - First save
        userPreferencesService.saveUserPreferences(preferences);

        // First retrieval - should hit cache
        Optional<UserPreferences> first = userPreferencesService.getUserPreferences(userId);
        
        // Second retrieval - should hit cache again
        Optional<UserPreferences> second = userPreferencesService.getUserPreferences(userId);

        // Then
        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get().getUserId()).isEqualTo(second.get().getUserId());
        assertThat(first.get().getTheme()).isEqualTo(second.get().getTheme());
    }

    @Test
    void shouldDeleteUserPreferences() {
        // Given
        String userId = "test-user-delete-999";
        UserPreferences preferences = UserPreferences.builder()
            .userId(userId)
            .theme("light")
            .language("en")
            .currency("USD")
            .timezone("UTC")
            .notificationsEnabled(true)
            .customSettings(new HashMap<>())
            .build();

        userPreferencesService.saveUserPreferences(preferences);
        assertThat(userPreferencesService.existsUserPreferences(userId)).isTrue();

        // When
        userPreferencesService.deleteUserPreferences(userId);

        // Then
        assertThat(userPreferencesService.existsUserPreferences(userId)).isFalse();
        Optional<UserPreferences> retrieved = userPreferencesService.getUserPreferences(userId);
        assertThat(retrieved).isEmpty();
    }
}
