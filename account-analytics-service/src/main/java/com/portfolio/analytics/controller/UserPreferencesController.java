package com.portfolio.analytics.controller;

import com.portfolio.analytics.model.UserPreferences;
import com.portfolio.analytics.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "User Preferences", description = "User preferences and settings API")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @Operation(summary = "Get user preferences", description = "Retrieve cached user preferences")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Preferences not found for user"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID format")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserPreferences> getUserPreferences(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId) {
        
        log.info("GET /api/v1/preferences/users/{}", userId);
        
        Optional<UserPreferences> preferences = userPreferencesService.getUserPreferences(userId);
        
        if (preferences.isPresent()) {
            return ResponseEntity.ok(preferences.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Create user preferences", 
               description = "Create new user preferences and cache them")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Preferences created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Preferences already exist for user")
    })
    @PostMapping("/users/{userId}")
    public ResponseEntity<UserPreferences> createUserPreferences(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId,
            @Parameter(description = "User preferences data", required = true)
            @RequestBody @Valid UserPreferences preferences) {
        
        log.info("POST /api/v1/preferences/users/{}", userId);
        
        // Check if preferences already exist
        if (userPreferencesService.existsUserPreferences(userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // Set the user ID from path parameter
        preferences.setUserId(userId);
        
        UserPreferences saved = userPreferencesService.saveUserPreferences(preferences);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Update user preferences", 
               description = "Update existing user preferences with caching")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @ApiResponse(responseCode = "404", description = "Preferences not found for user"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PutMapping("/users/{userId}")
    public ResponseEntity<UserPreferences> updateUserPreferences(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId,
            @Parameter(description = "Updated user preferences data", required = true)
            @RequestBody @Valid UserPreferences preferences) {
        
        log.info("PUT /api/v1/preferences/users/{}", userId);
        
        // Check if preferences exist
        if (!userPreferencesService.existsUserPreferences(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        // Set the user ID from path parameter
        preferences.setUserId(userId);
        
        UserPreferences updated = userPreferencesService.updateUserPreferences(userId, preferences);
        
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Partially update user preferences", 
               description = "Update specific fields of user preferences")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @ApiResponse(responseCode = "404", description = "Preferences not found for user"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserPreferences> patchUserPreferences(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId,
            @Parameter(description = "Partial user preferences data", required = true)
            @RequestBody UserPreferences preferences) {
        
        log.info("PATCH /api/v1/preferences/users/{}", userId);
        
        // Check if preferences exist
        if (!userPreferencesService.existsUserPreferences(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        UserPreferences updated = userPreferencesService.updateUserPreferences(userId, preferences);
        
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete user preferences", 
               description = "Remove user preferences and clear from cache")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Preferences deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Preferences not found for user"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID format")
    })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUserPreferences(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId) {
        
        log.info("DELETE /api/v1/preferences/users/{}", userId);
        
        // Check if preferences exist
        if (!userPreferencesService.existsUserPreferences(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        userPreferencesService.deleteUserPreferences(userId);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check preferences existence", 
               description = "Check if preferences exist for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID format")
    })
    @GetMapping("/users/{userId}/exists")
    public ResponseEntity<Boolean> preferencesExist(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId) {
        
        log.info("GET /api/v1/preferences/users/{}/exists", userId);
        
        boolean exists = userPreferencesService.existsUserPreferences(userId);
        
        return ResponseEntity.ok(exists);
    }

    @Operation(summary = "Get default preferences template", 
               description = "Get default preferences structure for reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Default preferences template retrieved")
    })
    @GetMapping("/template")
    public ResponseEntity<UserPreferences> getDefaultPreferencesTemplate() {
        
        log.info("GET /api/v1/preferences/template");
        
        UserPreferences template = UserPreferences.builder()
            .userId("example-user-id")
            .theme("light")
            .language("en")
            .currency("USD")
            .timezone("UTC")
            .notificationsEnabled(true)
            .build();
        
        return ResponseEntity.ok(template);
    }

    @Operation(summary = "Bulk update preferences", 
               description = "Update preferences for multiple users - Admin endpoint")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk update completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/bulk-update")
    public ResponseEntity<BulkUpdateResult> bulkUpdatePreferences(
            @Parameter(description = "Map of user preferences updates", required = true)
            @RequestBody @Valid BulkUpdateRequest request) {
        
        log.info("POST /api/v1/preferences/bulk-update for {} users", request.getUpdates().size());
        
        int successful = 0;
        int failed = 0;
        
        for (var entry : request.getUpdates().entrySet()) {
            try {
                String userId = entry.getKey();
                UserPreferences preferences = entry.getValue();
                
                if (userPreferencesService.existsUserPreferences(userId)) {
                    userPreferencesService.updateUserPreferences(userId, preferences);
                    successful++;
                } else {
                    preferences.setUserId(userId);
                    userPreferencesService.saveUserPreferences(preferences);
                    successful++;
                }
            } catch (Exception e) {
                log.error("Failed to update preferences for user {}: {}", entry.getKey(), e.getMessage());
                failed++;
            }
        }
        
        BulkUpdateResult result = BulkUpdateResult.builder()
            .successful(successful)
            .failed(failed)
            .total(request.getUpdates().size())
            .build();
        
        return ResponseEntity.ok(result);
    }

    // DTOs for bulk operations
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkUpdateRequest {
        private java.util.Map<String, UserPreferences> updates;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor  
    @lombok.AllArgsConstructor
    public static class BulkUpdateResult {
        private int successful;
        private int failed;
        private int total;
    }
}
