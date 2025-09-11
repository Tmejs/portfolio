package com.portfolio.demo.repository;

import com.portfolio.demo.AbstractRepositoryTest;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.AccountStatus;
import com.portfolio.demo.entity.AccountType;
import com.portfolio.demo.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Improved integration tests demonstrating better session management approaches
 * than class-level @Transactional annotation.
 */
@ActiveProfiles("test")
class AccountRepositoryImprovedTest extends AbstractRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        accountRepository.deleteAll();
        
        // Create test account
        testAccount = Account.builder()
            .accountNumber("ACC1234567890")
            .customerName("John Doe")
            .customerEmail("john.doe@example.com")
            .accountType(AccountType.CHECKING)
            .balance(new BigDecimal("1000.00"))
            .currency("USD")
            .status(AccountStatus.ACTIVE)
            .build();
    }

    // APPROACH 1: Method-level @Transactional (Best for tests with lazy loading)
    @Test
    @Transactional
    void shouldHandleLazyLoadingWithMethodTransaction() {
        // Given
        Account savedAccount = accountRepository.save(testAccount);
        
        // When - This will work with lazy loading because of @Transactional
        Account found = accountRepository.findById(savedAccount.getId()).orElseThrow();
        
        // Accessing lazy-loaded collections works within transaction
        List<Transaction> transactions = found.getTransactions(); // This would fail without proper session
        
        // Then
        assertThat(found.getCustomerName()).isEqualTo("John Doe");
        assertThat(transactions).isEmpty(); // No transactions yet
    }

    // APPROACH 2: EntityManager.flush() and refresh for immediate persistence
    @Test
    void shouldUseEntityManagerForImmediatePersistence() {
        // Given & When
        Account savedAccount = accountRepository.save(testAccount);
        entityManager.flush(); // Force immediate DB write
        entityManager.refresh(savedAccount); // Refresh from DB
        
        // Then
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getCreatedAt()).isNotNull();
    }

    // APPROACH 3: Repository methods that handle sessions properly
    @Test  
    void shouldUseRepositoryMethodsWithProperFetching() {
        // Given
        accountRepository.save(testAccount);
        
        // When - Using repository methods that handle sessions
        Optional<Account> found = accountRepository.findByAccountNumber("ACC1234567890");
        List<Account> accounts = accountRepository.findByCustomerEmail("john.doe@example.com");
        
        // Then - These work because they're complete operations within repository boundaries
        assertThat(found).isPresent();
        assertThat(accounts).hasSize(1);
        assertThat(found.get().getCustomerName()).isEqualTo("John Doe");
    }

    // APPROACH 4: Test-specific transaction management
    @Test
    void shouldManageTransactionsScopedToOperations() {
        // Given - Save in one transaction boundary
        Account savedAccount = accountRepository.save(testAccount);
        Long accountId = savedAccount.getId();
        
        // When - Query in separate transaction boundary (simulates real usage)
        Optional<Account> found = accountRepository.findById(accountId);
        long count = accountRepository.count();
        
        // Then
        assertThat(found).isPresent();
        assertThat(count).isEqualTo(1);
        assertThat(found.get().getBalance()).isEqualByComparingTo("1000.00");
    }

    // APPROACH 5: Use @Transactional only when testing complex scenarios
    @Test
    @Transactional
    void shouldTestComplexTransactionalBehavior() {
        // Given
        Account account1 = accountRepository.save(testAccount);
        
        Account account2 = Account.builder()
            .accountNumber("ACC0987654321")
            .customerName("Jane Smith")
            .customerEmail("jane.smith@example.com")
            .accountType(AccountType.SAVINGS)
            .balance(new BigDecimal("2000.00"))
            .currency("USD")
            .status(AccountStatus.ACTIVE)
            .build();
        accountRepository.save(account2);
        
        // When - Complex operations that need single transaction
        List<Account> allAccounts = accountRepository.findAll();
        long totalCount = accountRepository.count();
        
        // Modify within same transaction
        account1.setBalance(new BigDecimal("1500.00"));
        accountRepository.save(account1);
        
        // Then
        assertThat(allAccounts).hasSize(2);
        assertThat(totalCount).isEqualTo(2);
        
        // Verify changes are visible within same transaction
        Account updatedAccount = accountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("1500.00");
    }
}
