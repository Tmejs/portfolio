package com.portfolio.demo.enums;

/**
 * Enum representing different types of bank accounts
 */
public enum AccountType {
    CHECKING("Checking Account"),
    SAVINGS("Savings Account");
    
    private final String displayName;
    
    AccountType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
