package com.portfolio.demo.service;

import com.portfolio.demo.dto.message.AccountCreatedMessage;
import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.exception.AccountNotFoundException;
import com.portfolio.demo.exception.InvalidAccountOperationException;
import com.portfolio.demo.repository.AccountRepository;
import com.portfolio.demo.service.messaging.MessageProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final MessageProducerService messageProducerService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for customer: {}", request.getCustomerName());
        
        // Validate initial balance
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountOperationException("Initial balance cannot be negative");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .accountType(request.getAccountType())
                .balance(request.getInitialBalance())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Created account with ID: {} for customer: {}", savedAccount.getId(), request.getCustomerName());
        
        // Publish account created event
        publishAccountCreatedEvent(savedAccount);
        
        return mapToAccountResponse(savedAccount);
    }

    public AccountResponse getAccountById(Long accountId) {
        log.debug("Retrieving account with ID: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
        
        return mapToAccountResponse(account);
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        log.debug("Retrieving account with number: {}", accountNumber);
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
        
        return mapToAccountResponse(account);
    }

    public List<AccountResponse> getAllAccounts() {
        log.debug("Retrieving all accounts");
        
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    public List<AccountResponse> getActiveAccounts() {
        log.debug("Retrieving all active accounts");
        
        List<Account> accounts = accountRepository.findByStatus(AccountStatus.ACTIVE);
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    public List<AccountResponse> getAccountsByCustomerName(String customerName) {
        log.debug("Retrieving accounts for customer: {}", customerName);
        
        List<Account> accounts = accountRepository.findByCustomerNameContainingIgnoreCase(customerName);
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateAccountBalance(Long accountId, BigDecimal newBalance) {
        log.info("Updating balance for account ID: {} to: {}", accountId, newBalance);
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountOperationException("Account balance cannot be negative");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Cannot update balance for inactive account");
        }

        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        
        Account updatedAccount = accountRepository.save(account);
        log.info("Updated balance for account ID: {} to: {}", accountId, newBalance);
        
        return mapToAccountResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse deactivateAccount(Long accountId) {
        log.info("Deactivating account with ID: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Account is already inactive");
        }

        // Check if account has a balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidAccountOperationException("Cannot deactivate account with positive balance. Current balance: " + account.getBalance());
        }

        account.setStatus(AccountStatus.INACTIVE);
        account.setUpdatedAt(LocalDateTime.now());
        
        Account deactivatedAccount = accountRepository.save(account);
        log.info("Deactivated account with ID: {}", accountId);
        
        return mapToAccountResponse(deactivatedAccount);
    }

    @Transactional
    public AccountResponse reactivateAccount(Long accountId) {
        log.info("Reactivating account with ID: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Account is already active");
        }

        account.setStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(LocalDateTime.now());
        
        Account reactivatedAccount = accountRepository.save(account);
        log.info("Reactivated account with ID: {}", accountId);
        
        return mapToAccountResponse(reactivatedAccount);
    }

    // Package-private method for use by TransactionService
    Account findAccountEntityById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
    }

    // Package-private method for use by TransactionService
    @Transactional
    Account saveAccount(Account account) {
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerName(account.getCustomerName())
                .customerEmail(account.getCustomerEmail())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private void publishAccountCreatedEvent(Account account) {
        try {
            AccountCreatedMessage message = AccountCreatedMessage.builder()
                    .accountId(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .customerName(account.getCustomerName())
                    .customerEmail(account.getCustomerEmail())
                    .accountType(account.getAccountType())
                    .balance(account.getBalance())
                    .currency(account.getCurrency())
                    .status(account.getStatus())
                    .createdAt(account.getCreatedAt())
                    .eventTimestamp(LocalDateTime.now())
                    .build();
            
            messageProducerService.publishAccountCreated(message);
            log.debug("Successfully published account created event for account: {}", account.getAccountNumber());
            
        } catch (Exception e) {
            // Log error but don't fail the transaction - messaging is async
            log.error("Failed to publish account created event for account {}: {}", 
                     account.getAccountNumber(), e.getMessage(), e);
        }
    }

    private String generateAccountNumber() {
        // Simple account number generation - in production, this would be more sophisticated
        long timestamp = System.currentTimeMillis();
        long randomSuffix = (long) (Math.random() * 10000);
        return String.format("ACC%d%04d", timestamp % 1000000, randomSuffix);
    }
}
