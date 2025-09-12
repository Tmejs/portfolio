package com.portfolio.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("user_preferences")
public class UserPreferences implements Serializable {
    
    @Id
    private String userId;
    
    private String theme;
    
    private String language;
    
    private boolean notificationsEnabled;
    
    private String currency;
    
    private String timezone;
    
    private Map<String, Object> customSettings;
    
    private LocalDateTime lastUpdated;
    
    private LocalDateTime createdAt;
}
