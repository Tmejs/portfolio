package com.portfolio.demo.repository;

import com.portfolio.demo.AbstractIntegrationTest;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.AccountStatus;
import com.portfolio.demo.entity.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AccountRepository using real PostgreSQL database via Testcontainers
 * Designed for Java 24 banking microservices
 */
@ActiveProfiles("test")
class AccountRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        accountRepository.deleteAll();
        
        // Create test account
        testAccount = new Account();
        testAccount.setAccountNumber("ACC1234567890");
        testAccount.setCustomerName("John Doe");
        testAccount.setCustomerEmail("john.doe@example.com");
        testAccount.setAccountType(AccountType.CHECKING);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setCurrency("USD");
        testAccount.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    void shouldSaveAndFindAccount() {
        // Given - account is set up in @BeforeEach
        
        // When
        Account savedAccount = accountRepository.save(testAccount);
        
        // Then
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("ACC1234567890");
        assertThat(savedAccount.getCustomerName()).isEqualTo("John Doe");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(savedAccount.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindAccountByAccountNumber() {
        // Given
        accountRepository.save(testAccount);
        
        // When
        Optional<Account> found = accountRepository.findByAccountNumber("ACC1234567890");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void shouldFindAccountsByCustomerEmail() {
        // Given
        accountRepository.save(testAccount);
        
        Account secondAccount = new Account();
        secondAccount.setAccountNumber("ACC0987654321");
        secondAccount.setCustomerName("John Doe");
        secondAccount.setCustomerEmail("john.doe@example.com");
        secondAccount.setAccountType(AccountType.SAVINGS);
        secondAccount.setBalance(new BigDecimal("500.00"));
        secondAccount.setCurrency("USD");
        secondAccount.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(secondAccount);
        
        // When
        List<Account> accounts = accountRepository.findByCustomerEmail("john.doe@example.com");
        
        // Then
        assertThat(accounts).hasSize(2);
        assertThat(accounts)
            .extracting(Account::getAccountType)
            .containsExactlyInAnyOrder(AccountType.CHECKING, AccountType.SAVINGS);
    }

    @Test
    void shouldFindAccountsByAccountType() {
        // Given
        accountRepository.save(testAccount);
        
        Account savingsAccount = new Account();
        savingsAccount.setAccountNumber("SAV1234567890");
        savingsAccount.setCustomerName("Jane Smith");
        savingsAccount.setCustomerEmail("jane.smith@example.com");
        savingsAccount.setAccountType(AccountType.SAVINGS);
        savingsAccount.setBalance(new BigDecimal("2000.00"));
        savingsAccount.setCurrency("USD");
        savingsAccount.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(savingsAccount);
        
        // When
        List<Account> checkingAccounts = accountRepository.findByAccountType(AccountType.CHECKING);
        List<Account> savingsAccounts = accountRepository.findByAccountType(AccountType.SAVINGS);
        
        // Then
        assertThat(checkingAccounts).hasSize(1);
        assertThat(savingsAccounts).hasSize(1);
        assertThat(checkingAccounts.get(0).getCustomerName()).isEqualTo("John Doe");
        assertThat(savingsAccounts.get(0).getCustomerName()).isEqualTo("Jane Smith");
    }

    @Test
    void shouldCountAccountsByStatus() {
        // Given
        accountRepository.save(testAccount);
        
        Account inactiveAccount = new Account();
        inactiveAccount.setAccountNumber("INA1234567890");
        inactiveAccount.setCustomerName("Inactive User");
        inactiveAccount.setCustomerEmail("inactive@example.com");
        inactiveAccount.setAccountType(AccountType.SAVINGS);
        inactiveAccount.setBalance(new BigDecimal("100.00"));
        inactiveAccount.setCurrency("USD");
        inactiveAccount.setStatus(AccountStatus.INACTIVE);
        accountRepository.save(inactiveAccount);
        
        // When
        long activeCount = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long inactiveCount = accountRepository.countByStatus(AccountStatus.INACTIVE);
        
        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    void shouldCheckIfAccountNumberExists() {
        // Given
        accountRepository.save(testAccount);
        
        // When & Then
        assertThat(accountRepository.existsByAccountNumber("ACC1234567890")).isTrue();
        assertThat(accountRepository.existsByAccountNumber("NONEXISTENT")).isFalse();
    }
}
