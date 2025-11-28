package com.portfolio.demo.service;

import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.AccountType;
import com.portfolio.demo.exception.AccountNotFoundException;
import com.portfolio.demo.exception.InvalidAccountOperationException;
import com.portfolio.demo.repository.AccountRepository;
import com.portfolio.demo.service.messaging.MessageProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MessageProducerService messageProducerService;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private CreateAccountRequest createAccountRequest;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC123456")
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createAccountRequest = CreateAccountRequest.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();
    }

    @Test
    @DisplayName("Should create account successfully with valid request")
    void testCreateAccount_Success() {
        // Given
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse response = accountService.createAccount(createAccountRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getCustomerEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(messageProducerService, times(1)).publishAccountCreated(any());
    }

    @Test
    @DisplayName("Should throw exception when creating account with negative initial balance")
    void testCreateAccount_NegativeBalance_ThrowsException() {
        // Given
        createAccountRequest.setInitialBalance(new BigDecimal("-100.00"));

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(createAccountRequest))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Initial balance cannot be negative");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should use default currency USD when not specified")
    void testCreateAccount_DefaultCurrency() {
        // Given
        createAccountRequest.setCurrency(null);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        accountService.createAccount(createAccountRequest);

        // Then
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should retrieve account by ID successfully")
    void testGetAccountById_Success() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When
        AccountResponse response = accountService.getAccountById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCustomerName()).isEqualTo("John Doe");

        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account ID not found")
    void testGetAccountById_NotFound_ThrowsException() {
        // Given
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountById(999L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");

        verify(accountRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should retrieve account by account number successfully")
    void testGetAccountByAccountNumber_Success() {
        // Given
        when(accountRepository.findByAccountNumber("ACC123456")).thenReturn(Optional.of(testAccount));

        // When
        AccountResponse response = accountService.getAccountByAccountNumber("ACC123456");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("ACC123456");

        verify(accountRepository, times(1)).findByAccountNumber("ACC123456");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account number not found")
    void testGetAccountByAccountNumber_NotFound_ThrowsException() {
        // Given
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber("INVALID"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with number: INVALID");
    }

    @Test
    @DisplayName("Should retrieve all accounts successfully")
    void testGetAllAccounts_Success() {
        // Given
        Account account2 = Account.builder()
                .id(2L)
                .accountNumber("ACC789012")
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findAll()).thenReturn(Arrays.asList(testAccount, account2));

        // When
        List<AccountResponse> responses = accountService.getAllAccounts();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getCustomerName()).isEqualTo("John Doe");
        assertThat(responses.get(1).getCustomerName()).isEqualTo("Jane Smith");

        verify(accountRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should retrieve only active accounts")
    void testGetActiveAccounts_Success() {
        // Given
        when(accountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(Arrays.asList(testAccount));

        // When
        List<AccountResponse> responses = accountService.getActiveAccounts();

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository, times(1)).findByStatus(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should retrieve accounts by customer name")
    void testGetAccountsByCustomerName_Success() {
        // Given
        when(accountRepository.findByCustomerNameContainingIgnoreCase("John"))
                .thenReturn(Arrays.asList(testAccount));

        // When
        List<AccountResponse> responses = accountService.getAccountsByCustomerName("John");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerName()).contains("John");

        verify(accountRepository, times(1)).findByCustomerNameContainingIgnoreCase("John");
    }

    @Test
    @DisplayName("Should update account balance successfully")
    void testUpdateAccountBalance_Success() {
        // Given
        BigDecimal newBalance = new BigDecimal("1500.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse response = accountService.updateAccountBalance(1L, newBalance);

        // Then
        assertThat(response).isNotNull();
        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when updating balance to negative value")
    void testUpdateAccountBalance_NegativeBalance_ThrowsException() {
        // Given
        BigDecimal negativeBalance = new BigDecimal("-100.00");

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccountBalance(1L, negativeBalance))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Account balance cannot be negative");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when updating balance for inactive account")
    void testUpdateAccountBalance_InactiveAccount_ThrowsException() {
        // Given
        testAccount.setStatus(AccountStatus.INACTIVE);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccountBalance(1L, new BigDecimal("1500.00")))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Cannot update balance for inactive account");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should deactivate account successfully when balance is zero")
    void testDeactivateAccount_Success() {
        // Given
        testAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse response = accountService.deactivateAccount(1L);

        // Then
        assertThat(response).isNotNull();
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when deactivating account with positive balance")
    void testDeactivateAccount_PositiveBalance_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.deactivateAccount(1L))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Cannot deactivate account with positive balance");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when deactivating already inactive account")
    void testDeactivateAccount_AlreadyInactive_ThrowsException() {
        // Given
        testAccount.setStatus(AccountStatus.INACTIVE);
        testAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.deactivateAccount(1L))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Account is already inactive");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reactivate account successfully")
    void testReactivateAccount_Success() {
        // Given
        testAccount.setStatus(AccountStatus.INACTIVE);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse response = accountService.reactivateAccount(1L);

        // Then
        assertThat(response).isNotNull();
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when reactivating already active account")
    void testReactivateAccount_AlreadyActive_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.reactivateAccount(1L))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Account is already active");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should find account entity by ID for internal use")
    void testFindAccountEntityById_Success() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When
        Account account = accountService.findAccountEntityById(1L);

        // Then
        assertThat(account).isNotNull();
        assertThat(account.getId()).isEqualTo(1L);

        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should save account entity for internal use")
    void testSaveAccount_Success() {
        // Given
        when(accountRepository.save(testAccount)).thenReturn(testAccount);

        // When
        Account savedAccount = accountService.saveAccount(testAccount);

        // Then
        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getUpdatedAt()).isNotNull();

        verify(accountRepository, times(1)).save(testAccount);
    }
}
