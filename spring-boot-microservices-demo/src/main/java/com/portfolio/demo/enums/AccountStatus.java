package com.portfolio.demo.enums;

/**
 * Enum representing the status of a bank account
 */
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    CLOSED("Closed"),
    FROZEN("Frozen");
    
    private final String displayName;
    
    AccountStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
