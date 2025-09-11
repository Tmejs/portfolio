package com.portfolio.demo.exception;

/**
 * Base exception for banking operations
 */
public class BankingException extends RuntimeException {
    
    public BankingException(String message) {
        super(message);
    }
    
    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when account is not found
 */
class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}

/**
 * Exception thrown when transaction fails due to insufficient funds
 */
class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String message) {
        super("Insufficient funds: " + message);
    }
}

/**
 * Exception thrown when account number already exists
 */
class AccountAlreadyExistsException extends BankingException {
    public AccountAlreadyExistsException(String accountNumber) {
        super("Account already exists: " + accountNumber);
    }
}

/**
 * Exception thrown when transaction reference number already exists
 */
class TransactionAlreadyExistsException extends BankingException {
    public TransactionAlreadyExistsException(String referenceNumber) {
        super("Transaction already exists: " + referenceNumber);
    }
}
