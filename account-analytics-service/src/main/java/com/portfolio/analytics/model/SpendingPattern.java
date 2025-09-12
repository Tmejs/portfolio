package com.portfolio.analytics.model;

/**
 * Enum representing different spending patterns detected in account analytics
 */
public enum SpendingPattern {
    /**
     * Consistent spending amounts with low volatility
     */
    STABLE,
    
    /**
     * Varying spending amounts with moderate volatility
     */
    VARIABLE,
    
    /**
     * High spending amounts with high volatility
     */
    VOLATILE,
    
    /**
     * Gradual increase in spending over time
     */
    INCREASING,
    
    /**
     * Gradual decrease in spending over time
     */
    DECREASING,
    
    /**
     * Irregular spending pattern with unpredictable amounts
     */
    ERRATIC,
    
    /**
     * Seasonal or cyclical spending pattern
     */
    SEASONAL
}
