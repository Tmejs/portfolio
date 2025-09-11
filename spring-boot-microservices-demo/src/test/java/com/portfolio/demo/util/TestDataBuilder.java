package com.portfolio.demo.util;

import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.AccountStatus;
import com.portfolio.demo.entity.AccountType;
import com.portfolio.demo.entity.Transaction;
import com.portfolio.demo.entity.TransactionType;

import java.math.BigDecimal;

/**
 * Utility class for building test data objects
 * This avoids issues with Lombok in Java 24 preview
 */
public class TestDataBuilder {
    
    public static Account createTestAccount() {
        Account account = new Account();
        // Using reflection to set fields temporarily until Lombok is fixed
        try {
            var accountNumberField = Account.class.getDeclaredField("accountNumber");
            accountNumberField.setAccessible(true);
            accountNumberField.set(account, "ACC1234567890");
            
            var customerNameField = Account.class.getDeclaredField("customerName");
            customerNameField.setAccessible(true);
            customerNameField.set(account, "John Doe");
            
            var customerEmailField = Account.class.getDeclaredField("customerEmail");
            customerEmailField.setAccessible(true);
            customerEmailField.set(account, "john.doe@example.com");
            
            var accountTypeField = Account.class.getDeclaredField("accountType");
            accountTypeField.setAccessible(true);
            accountTypeField.set(account, AccountType.CHECKING);
            
            var balanceField = Account.class.getDeclaredField("balance");
            balanceField.setAccessible(true);
            balanceField.set(account, new BigDecimal("1000.00"));
            
            var currencyField = Account.class.getDeclaredField("currency");
            currencyField.setAccessible(true);
            currencyField.set(account, "USD");
            
            var statusField = Account.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(account, AccountStatus.ACTIVE);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test account", e);
        }
        return account;
    }
    
    public static Transaction createTestTransaction(Account account) {
        Transaction transaction = new Transaction();
        // Using reflection to set fields temporarily until Lombok is fixed
        try {
            var referenceNumberField = Transaction.class.getDeclaredField("referenceNumber");
            referenceNumberField.setAccessible(true);
            referenceNumberField.set(transaction, "TXN1234567890");
            
            var transactionTypeField = Transaction.class.getDeclaredField("transactionType");
            transactionTypeField.setAccessible(true);
            transactionTypeField.set(transaction, TransactionType.DEPOSIT);
            
            var amountField = Transaction.class.getDeclaredField("amount");
            amountField.setAccessible(true);
            amountField.set(transaction, new BigDecimal("100.00"));
            
            var descriptionField = Transaction.class.getDeclaredField("description");
            descriptionField.setAccessible(true);
            descriptionField.set(transaction, "Test deposit");
            
            var balanceAfterField = Transaction.class.getDeclaredField("balanceAfter");
            balanceAfterField.setAccessible(true);
            balanceAfterField.set(transaction, new BigDecimal("1100.00"));
            
            var accountField = Transaction.class.getDeclaredField("account");
            accountField.setAccessible(true);
            accountField.set(transaction, account);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test transaction", e);
        }
        return transaction;
    }
}
