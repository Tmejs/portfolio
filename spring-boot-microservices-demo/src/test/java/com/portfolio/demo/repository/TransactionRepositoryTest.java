package com.portfolio.demo.repository;

import com.portfolio.demo.AbstractRepositoryTest;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.AccountStatus;
import com.portfolio.demo.entity.AccountType;
import com.portfolio.demo.entity.Transaction;
import com.portfolio.demo.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TransactionRepository using real PostgreSQL database via Testcontainers
 * Uses improved session management without class-level @Transactional
 */
@ActiveProfiles("test")
class TransactionRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Create test account using Lombok builder
        testAccount = Account.builder()
            .accountNumber("ACC1234567890")
            .customerName("John Doe")
            .customerEmail("john.doe@example.com")
            .accountType(AccountType.CHECKING)
            .balance(new BigDecimal("1000.00"))
            .currency("USD")
            .status(AccountStatus.ACTIVE)
            .build();
        testAccount = accountRepository.save(testAccount);
        
        // Create test transaction using Lombok builder
        testTransaction = Transaction.builder()
            .referenceNumber("TXN1234567890")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("100.00"))
            .description("Test deposit")
            .balanceAfter(new BigDecimal("1100.00"))
            .account(testAccount)
            .build();
    }

    @Test
    void shouldSaveAndFindTransaction() {
        // When
        Transaction savedTransaction = transactionRepository.save(testTransaction);
        
        // Then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getReferenceNumber()).isEqualTo("TXN1234567890");
        assertThat(savedTransaction.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo("100.00");
        assertThat(savedTransaction.getCreatedAt()).isNotNull();
        assertThat(savedTransaction.getAccount().getId()).isEqualTo(testAccount.getId());
    }

    @Test
    void shouldFindTransactionByReferenceNumber() {
        // Given
        transactionRepository.save(testTransaction);
        
        // When
        Optional<Transaction> found = transactionRepository.findByReferenceNumber("TXN1234567890");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Test deposit");
        assertThat(found.get().getAccount().getAccountNumber()).isEqualTo("ACC1234567890");
    }

    @Test
    void shouldFindTransactionsByAccountId() {
        // Given
        transactionRepository.save(testTransaction);
        
        Transaction withdrawal = Transaction.builder()
            .referenceNumber("TXN0987654321")
            .transactionType(TransactionType.WITHDRAWAL)
            .amount(new BigDecimal("50.00"))
            .description("Test withdrawal")
            .balanceAfter(new BigDecimal("1050.00"))
            .account(testAccount)
            .build();
        transactionRepository.save(withdrawal);
        
        // When
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(testAccount.getId());
        
        // Then
        assertThat(transactions).hasSize(2);
        // Should be ordered by created date descending (latest first)
        assertThat(transactions.get(0).getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transactions.get(1).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void shouldFindTransactionsByAccountNumber() {
        // Given
        transactionRepository.save(testTransaction);
        
        // When
        List<Transaction> transactions = transactionRepository.findByAccountNumber("ACC1234567890");
        
        // Then
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getReferenceNumber()).isEqualTo("TXN1234567890");
    }

    @Test
    void shouldFindTransactionsByType() {
        // Given
        transactionRepository.save(testTransaction);
        
        Transaction withdrawal = Transaction.builder()
            .referenceNumber("TXN0987654321")
            .transactionType(TransactionType.WITHDRAWAL)
            .amount(new BigDecimal("50.00"))
            .description("Test withdrawal")
            .balanceAfter(new BigDecimal("950.00"))
            .account(testAccount)
            .build();
        transactionRepository.save(withdrawal);
        
        // When
        List<Transaction> deposits = transactionRepository.findByTransactionType(TransactionType.DEPOSIT);
        List<Transaction> withdrawals = transactionRepository.findByTransactionType(TransactionType.WITHDRAWAL);
        
        // Then
        assertThat(deposits).hasSize(1);
        assertThat(withdrawals).hasSize(1);
        assertThat(deposits.get(0).getAmount()).isEqualByComparingTo("100.00");
        assertThat(withdrawals.get(0).getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldGetTotalAmountByTypeAndAccount() {
        // Given
        transactionRepository.save(testTransaction);
        
        Transaction anotherDeposit = Transaction.builder()
            .referenceNumber("TXN2222222222")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("200.00"))
            .description("Another deposit")
            .balanceAfter(new BigDecimal("1300.00"))
            .account(testAccount)
            .build();
        transactionRepository.save(anotherDeposit);
        
        // When
        BigDecimal totalDeposits = transactionRepository.getTotalAmountByTypeAndAccount(
            testAccount.getId(), TransactionType.DEPOSIT);
        
        // Then
        assertThat(totalDeposits).isEqualByComparingTo("300.00"); // 100 + 200
    }

    @Test
    void shouldCountTransactionsByAccountAndType() {
        // Given
        transactionRepository.save(testTransaction);
        
        Transaction withdrawal = Transaction.builder()
            .referenceNumber("TXN3333333333")
            .transactionType(TransactionType.WITHDRAWAL)
            .amount(new BigDecimal("50.00"))
            .description("Test withdrawal")
            .balanceAfter(new BigDecimal("1050.00"))
            .account(testAccount)
            .build();
        transactionRepository.save(withdrawal);
        
        // When
        long depositCount = transactionRepository.countByAccountAndType(testAccount.getId(), TransactionType.DEPOSIT);
        long withdrawalCount = transactionRepository.countByAccountAndType(testAccount.getId(), TransactionType.WITHDRAWAL);
        
        // Then
        assertThat(depositCount).isEqualTo(1);
        assertThat(withdrawalCount).isEqualTo(1);
    }

    @Test
    void shouldCheckIfReferenceNumberExists() {
        // Given
        transactionRepository.save(testTransaction);
        
        // When & Then
        assertThat(transactionRepository.existsByReferenceNumber("TXN1234567890")).isTrue();
        assertThat(transactionRepository.existsByReferenceNumber("NONEXISTENT")).isFalse();
    }

    @Test
    void shouldFindRecentTransactions() {
        // Given
        transactionRepository.save(testTransaction);
        
        // When
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        List<Transaction> recentTransactions = transactionRepository.findRecentTransactions(oneDayAgo);
        
        // Then
        assertThat(recentTransactions).hasSize(1);
        assertThat(recentTransactions.get(0).getReferenceNumber()).isEqualTo("TXN1234567890");
    }
}
