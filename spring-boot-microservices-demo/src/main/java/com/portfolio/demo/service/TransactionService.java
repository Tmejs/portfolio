package com.portfolio.demo.service;

import com.portfolio.demo.dto.request.CreateTransactionRequest;
import com.portfolio.demo.dto.response.TransactionResponse;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.Transaction;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.TransactionType;
import com.portfolio.demo.exception.AccountNotFoundException;
import com.portfolio.demo.exception.InsufficientFundsException;
import com.portfolio.demo.exception.InvalidTransactionException;
import com.portfolio.demo.exception.TransactionNotFoundException;
import com.portfolio.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        log.info("Creating {} transaction for account ID: {} with amount: {}", 
                request.getTransactionType(), request.getAccountId(), request.getAmount());

        // Validate transaction amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than zero");
        }

        // Get account entity
        Account account = accountService.findAccountEntityById(request.getAccountId());
        
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot perform transaction on inactive account");
        }

        // Calculate new balance based on transaction type
        BigDecimal newBalance = calculateNewBalance(account.getBalance(), request.getAmount(), request.getTransactionType());
        
        // Validate sufficient funds for withdrawals
        if (request.getTransactionType() == TransactionType.WITHDRAWAL && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                String.format("Insufficient funds. Current balance: %s, Withdrawal amount: %s", 
                    account.getBalance(), request.getAmount())
            );
        }

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .balanceBefore(account.getBalance())
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .referenceNumber(generateTransactionReferenceNumber())
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Update account balance
        account.setBalance(newBalance);
        accountService.saveAccount(account);

        log.info("Created {} transaction ID: {} for account ID: {}. Balance changed from {} to {}", 
                request.getTransactionType(), savedTransaction.getId(), account.getId(), 
                transaction.getBalanceBefore(), transaction.getBalanceAfter());

        return mapToTransactionResponse(savedTransaction);
    }

    public TransactionResponse getTransactionById(Long transactionId) {
        log.debug("Retrieving transaction with ID: {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));
        
        return mapToTransactionResponse(transaction);
    }

    public List<TransactionResponse> getTransactionsByAccountId(Long accountId) {
        log.debug("Retrieving transactions for account ID: {}", accountId);
        
        // Verify account exists
        accountService.findAccountEntityById(accountId);
        
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public Page<TransactionResponse> getTransactionsByAccountId(Long accountId, Pageable pageable) {
        log.debug("Retrieving transactions for account ID: {} with pagination: {}", accountId, pageable);
        
        // Verify account exists
        accountService.findAccountEntityById(accountId);
        
        Page<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        return transactions.map(this::mapToTransactionResponse);
    }

    public List<TransactionResponse> getTransactionsByAccountIdAndType(Long accountId, TransactionType transactionType) {
        log.debug("Retrieving {} transactions for account ID: {}", transactionType, accountId);
        
        // Verify account exists
        accountService.findAccountEntityById(accountId);
        
        List<Transaction> transactions = transactionRepository.findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(accountId, transactionType);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getRecentTransactions(int limit) {
        log.debug("Retrieving {} most recent transactions", limit);
        
        List<Transaction> transactions = transactionRepository.findRecentTransactions(limit);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public BigDecimal calculateAccountBalance(Long accountId) {
        log.debug("Calculating current balance for account ID: {}", accountId);
        
        // Verify account exists
        Account account = accountService.findAccountEntityById(accountId);
        
        // Get balance from account entity (this should match calculated balance from transactions)
        BigDecimal currentBalance = account.getBalance();
        
        // Optional: Verify balance consistency with transaction history
        BigDecimal calculatedBalance = transactionRepository.calculateCurrentBalance(accountId);
        if (calculatedBalance.compareTo(currentBalance) != 0) {
            log.warn("Balance inconsistency detected for account ID: {}. Account balance: {}, Calculated balance: {}", 
                    accountId, currentBalance, calculatedBalance);
        }
        
        return currentBalance;
    }

    @Transactional
    public TransactionResponse createDepositTransaction(Long accountId, BigDecimal amount, String description) {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .description(description != null ? description : "Deposit transaction")
                .build();
        
        return createTransaction(request);
    }

    @Transactional
    public TransactionResponse createWithdrawalTransaction(Long accountId, BigDecimal amount, String description) {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(amount)
                .description(description != null ? description : "Withdrawal transaction")
                .build();
        
        return createTransaction(request);
    }

    @Transactional
    public void processTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {
        log.info("Processing transfer of {} from account ID: {} to account ID: {}", amount, fromAccountId, toAccountId);
        
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }
        
        // Verify both accounts exist and are active
        Account fromAccount = accountService.findAccountEntityById(fromAccountId);
        Account toAccount = accountService.findAccountEntityById(toAccountId);
        
        if (fromAccount.getStatus() != AccountStatus.ACTIVE || toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Both accounts must be active for transfers");
        }
        
        String transferDescription = description != null ? description : String.format("Transfer between accounts");
        
        // Create withdrawal transaction (this will validate sufficient funds)
        createWithdrawalTransaction(fromAccountId, amount, 
                transferDescription + " - Transfer out to " + toAccount.getAccountNumber());
        
        // Create deposit transaction
        createDepositTransaction(toAccountId, amount, 
                transferDescription + " - Transfer in from " + fromAccount.getAccountNumber());
        
        log.info("Successfully processed transfer of {} from account ID: {} to account ID: {}", 
                amount, fromAccountId, toAccountId);
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal transactionAmount, TransactionType transactionType) {
        return switch (transactionType) {
            case DEPOSIT, TRANSFER_IN -> currentBalance.add(transactionAmount);
            case WITHDRAWAL, TRANSFER_OUT -> currentBalance.subtract(transactionAmount);
        };
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .accountNumber(transaction.getAccount().getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private String generateTransactionReferenceNumber() {
        // Simple transaction reference number generation - in production, this would be more sophisticated
        long timestamp = System.currentTimeMillis();
        long randomSuffix = (long) (Math.random() * 10000);
        return String.format("TXN%d%04d", timestamp % 1000000, randomSuffix);
    }
}
