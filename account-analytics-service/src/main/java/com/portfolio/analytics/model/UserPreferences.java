package com.portfolio.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_preferences")
@CompoundIndex(name = "user_updated_idx", def = "{'userId': 1, 'lastUpdated': -1}")
@CompoundIndex(name = "theme_lang_idx", def = "{'theme': 1, 'language': 1}")
public class UserPreferences implements Serializable {
    
    /**
     * Unique user identifier - serves as both MongoDB _id and Redis key
     */
    @Id
    @Field("_id")
    @Indexed(unique = true)
    private String userId;
    
    /**
     * UI theme preference (light, dark, auto)
     */
    @Field("theme")
    @Indexed
    private String theme;
    
    /**
     * Language preference (en, es, fr, etc.)
     */
    @Field("language")
    @Indexed
    private String language;
    
    /**
     * Whether user wants to receive notifications
     */
    @Field("notifications_enabled")
    @Indexed
    private boolean notificationsEnabled;
    
    /**
     * Currency preference (USD, EUR, GBP, etc.)
     */
    @Field("currency")
    @Indexed
    private String currency;
    
    /**
     * Timezone preference
     */
    @Field("timezone")
    private String timezone;
    
    /**
     * Additional custom settings as key-value pairs
     */
    @Field("custom_settings")
    private Map<String, Object> customSettings;
    
    /**
     * Timestamp when preferences were last updated
     */
    @Field("last_updated")
    @Indexed
    private LocalDateTime lastUpdated;
    
    /**
     * Timestamp when preferences were created
     */
    @Field("created_at")
    private LocalDateTime createdAt;
}
