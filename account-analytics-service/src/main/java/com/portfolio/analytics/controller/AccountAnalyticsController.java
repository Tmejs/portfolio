package com.portfolio.analytics.controller;

import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.service.AccountAnalyticsService;
import com.portfolio.analytics.service.AccountAnalyticsService.TransactionData;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Account Analytics", description = "Account analytics and metrics API")
public class AccountAnalyticsController {

    private final AccountAnalyticsService analyticsService;

    @Operation(summary = "Get account analytics", description = "Retrieve cached analytics for a specific account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Analytics not found for account"),
        @ApiResponse(responseCode = "400", description = "Invalid account ID format")
    })
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountAnalytics> getAccountAnalytics(
            @Parameter(description = "Account ID", required = true)
            @PathVariable @NotBlank String accountId) {
        
        log.info("GET /api/v1/analytics/accounts/{}", accountId);
        
        Optional<AccountAnalytics> analytics = analyticsService.getAccountAnalytics(accountId);
        
        if (analytics.isPresent()) {
            return ResponseEntity.ok(analytics.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Compute account analytics", 
               description = "Compute and cache analytics from provided transaction data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Analytics computed and cached successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "422", description = "Invalid transaction data format")
    })
    @PostMapping("/accounts/{accountId}/compute")
    public ResponseEntity<AccountAnalytics> computeAnalytics(
            @Parameter(description = "Account ID", required = true)
            @PathVariable @NotBlank String accountId,
            @Parameter(description = "Transaction data for analytics computation", required = true)
            @RequestBody @Valid List<TransactionData> transactions) {
        
        log.info("POST /api/v1/analytics/accounts/{}/compute with {} transactions", accountId, transactions.size());
        
        AccountAnalytics analytics = analyticsService.computeAnalytics(accountId, transactions);
        AccountAnalytics saved = analyticsService.saveAccountAnalytics(analytics);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Warm analytics cache", 
               description = "Pre-compute and cache analytics for faster retrieval")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache warmed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/accounts/{accountId}/warm-cache")
    public ResponseEntity<Void> warmCache(
            @Parameter(description = "Account ID", required = true)
            @PathVariable @NotBlank String accountId,
            @Parameter(description = "Transaction data for cache warming", required = true)
            @RequestBody @Valid List<TransactionData> transactions) {
        
        log.info("POST /api/v1/analytics/accounts/{}/warm-cache", accountId);
        
        analyticsService.warmCache(accountId, transactions);
        
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Invalidate analytics cache", 
               description = "Remove analytics from cache to force recomputation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cache invalidated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid account ID format")
    })
    @DeleteMapping("/accounts/{accountId}/cache")
    public ResponseEntity<Void> invalidateCache(
            @Parameter(description = "Account ID", required = true)
            @PathVariable @NotBlank String accountId) {
        
        log.info("DELETE /api/v1/analytics/accounts/{}/cache", accountId);
        
        analyticsService.invalidateAnalytics(accountId);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete account analytics", 
               description = "Remove all analytics data for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Analytics deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid account ID format")
    })
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> deleteAnalytics(
            @Parameter(description = "Account ID", required = true)
            @PathVariable @NotBlank String accountId) {
        
        log.info("DELETE /api/v1/analytics/accounts/{}", accountId);
        
        analyticsService.deleteAnalytics(accountId);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check analytics existence", 
               description = "Check if analytics exist for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed"),
        @ApiResponse(responseCode = "400", description = "Invalid account ID format")
    })
    @GetMapping("/accounts/{accountId}/exists")
    public ResponseEntity<Boolean> analyticsExists(
            @Parameter(description = "Account ID", required = true)
            @PathVariable @NotBlank String accountId) {
        
        log.info("GET /api/v1/analytics/accounts/{}/exists", accountId);
        
        boolean exists = analyticsService.analyticsExist(accountId);
        
        return ResponseEntity.ok(exists);
    }

    @Operation(summary = "Get analytics by spending pattern", 
               description = "Retrieve analytics for accounts with specific spending patterns")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid spending pattern")
    })
    @GetMapping("/spending-patterns/{pattern}")
    public ResponseEntity<List<AccountAnalytics>> getAnalyticsBySpendingPattern(
            @Parameter(description = "Spending pattern (CONSERVATIVE, MODERATE, AGGRESSIVE, INACTIVE)", required = true)
            @PathVariable @NotBlank String pattern) {
        
        log.info("GET /api/v1/analytics/spending-patterns/{}", pattern);
        
        List<AccountAnalytics> analytics = analyticsService.getAnalyticsBySpendingPattern(pattern.toUpperCase());
        
        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Get analytics by category", 
               description = "Retrieve analytics for accounts with specific primary categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category")
    })
    @GetMapping("/categories/{category}")
    public ResponseEntity<List<AccountAnalytics>> getAnalyticsByCategory(
            @Parameter(description = "Primary transaction category", required = true)
            @PathVariable @NotBlank String category) {
        
        log.info("GET /api/v1/analytics/categories/{}", category);
        
        List<AccountAnalytics> analytics = analyticsService.getAnalyticsByCategory(category.toUpperCase());
        
        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Get recently updated analytics", 
               description = "Retrieve analytics that have been updated since a specific time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date format")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<AccountAnalytics>> getRecentlyUpdatedAnalytics(
            @Parameter(description = "ISO datetime since when to fetch updates (e.g., 2024-01-01T10:00:00)")
            @RequestParam("since") LocalDateTime since) {
        
        log.info("GET /api/v1/analytics/recent?since={}", since);
        
        List<AccountAnalytics> analytics = analyticsService.getRecentlyUpdatedAnalytics(since);
        
        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Invalidate all analytics cache", 
               description = "Remove all analytics from cache - use with caution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "All cache invalidated successfully")
    })
    @DeleteMapping("/cache/all")
    public ResponseEntity<Void> invalidateAllCache() {
        
        log.warn("DELETE /api/v1/analytics/cache/all - Invalidating ALL analytics cache");
        
        analyticsService.invalidateAllAnalytics();
        
        return ResponseEntity.noContent().build();
    }
}
