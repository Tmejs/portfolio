package com.portfolio.demo.service;

import com.portfolio.demo.dto.request.CreateTransactionRequest;
import com.portfolio.demo.dto.response.TransactionResponse;
import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.Transaction;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.AccountType;
import com.portfolio.demo.enums.TransactionType;
import com.portfolio.demo.exception.InsufficientFundsException;
import com.portfolio.demo.exception.InvalidTransactionException;
import com.portfolio.demo.exception.TransactionNotFoundException;
import com.portfolio.demo.repository.TransactionRepository;
import com.portfolio.demo.service.messaging.MessageProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.
 * Tests transaction creation, validation, and business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private MessageProducerService messageProducerService;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Transaction testTransaction;
    private CreateTransactionRequest depositRequest;

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
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .description("Test deposit")
                .referenceNumber("TXN123456")
                .createdAt(LocalDateTime.now())
                .build();

        depositRequest = CreateTransactionRequest.builder()
                .accountId(1L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .description("Test deposit")
                .build();
    }

    @Test
    @DisplayName("Should create deposit transaction successfully")
    void testCreateTransaction_Deposit_Success() {
        // Given
        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountService.saveAccount(any(Account.class))).thenReturn(testAccount);

        // When
        TransactionResponse response = transactionService.createTransaction(depositRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountService, times(1)).saveAccount(any(Account.class));
        verify(messageProducerService, times(1)).publishTransactionCreated(any());
    }

    @Test
    @DisplayName("Should create withdrawal transaction successfully")
    void testCreateTransaction_Withdrawal_Success() {
        // Given
        CreateTransactionRequest withdrawalRequest = CreateTransactionRequest.builder()
                .accountId(1L)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("300.00"))
                .description("Test withdrawal")
                .build();

        Transaction withdrawalTransaction = Transaction.builder()
                .id(2L)
                .account(testAccount)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("300.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("700.00"))
                .description("Test withdrawal")
                .referenceNumber("TXN789012")
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(withdrawalTransaction);
        when(accountService.saveAccount(any(Account.class))).thenReturn(testAccount);

        // When
        TransactionResponse response = transactionService.createTransaction(withdrawalRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountService, times(1)).saveAccount(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when transaction amount is zero")
    void testCreateTransaction_ZeroAmount_ThrowsException() {
        // Given
        depositRequest.setAmount(BigDecimal.ZERO);

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(depositRequest))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction amount must be greater than zero");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when transaction amount is negative")
    void testCreateTransaction_NegativeAmount_ThrowsException() {
        // Given
        depositRequest.setAmount(new BigDecimal("-100.00"));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(depositRequest))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction amount must be greater than zero");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when account is inactive")
    void testCreateTransaction_InactiveAccount_ThrowsException() {
        // Given
        testAccount.setStatus(AccountStatus.INACTIVE);
        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(depositRequest))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Cannot perform transaction on inactive account");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when withdrawal exceeds balance")
    void testCreateTransaction_InsufficientFunds_ThrowsException() {
        // Given
        CreateTransactionRequest withdrawalRequest = CreateTransactionRequest.builder()
                .accountId(1L)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("1500.00")) // More than balance
                .description("Test withdrawal")
                .build();

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(withdrawalRequest))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should retrieve transaction by ID successfully")
    void testGetTransactionById_Success() {
        // Given
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // When
        TransactionResponse response = transactionService.getTransactionById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getReferenceNumber()).isEqualTo("TXN123456");

        verify(transactionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction not found")
    void testGetTransactionById_NotFound_ThrowsException() {
        // Given
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 999");
    }

    @Test
    @DisplayName("Should retrieve all transactions for an account")
    void testGetTransactionsByAccountId_Success() {
        // Given
        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .account(testAccount)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("200.00"))
                .balanceBefore(new BigDecimal("1500.00"))
                .balanceAfter(new BigDecimal("1300.00"))
                .description("Test withdrawal")
                .referenceNumber("TXN789012")
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(testTransaction, transaction2));

        // When
        List<TransactionResponse> responses = transactionService.getTransactionsByAccountId(1L);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(responses.get(1).getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);

        verify(transactionRepository, times(1)).findByAccountIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("Should retrieve transactions with pagination")
    void testGetTransactionsByAccountId_WithPagination_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(testTransaction));

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(transactionPage);

        // When
        Page<TransactionResponse> responses = transactionService.getTransactionsByAccountId(1L, pageable);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.getContent().get(0).getId()).isEqualTo(1L);

        verify(transactionRepository, times(1)).findByAccountIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    @DisplayName("Should retrieve transactions by account and type")
    void testGetTransactionsByAccountIdAndType_Success() {
        // Given
        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(1L, TransactionType.DEPOSIT))
                .thenReturn(Arrays.asList(testTransaction));

        // When
        List<TransactionResponse> responses = transactionService.getTransactionsByAccountIdAndType(1L,
                TransactionType.DEPOSIT);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);

        verify(transactionRepository, times(1))
                .findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(1L, TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("Should retrieve recent transactions")
    void testGetRecentTransactions_Success() {
        // Given
        when(transactionRepository.findRecentTransactions(10))
                .thenReturn(Arrays.asList(testTransaction));

        // When
        List<TransactionResponse> responses = transactionService.getRecentTransactions(10);

        // Then
        assertThat(responses).hasSize(1);
        verify(transactionRepository, times(1)).findRecentTransactions(10);
    }

    @Test
    @DisplayName("Should calculate account balance correctly")
    void testCalculateAccountBalance_Success() {
        // Given
        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.calculateCurrentBalance(1L))
                .thenReturn(new BigDecimal("1000.00"));

        // When
        BigDecimal balance = transactionService.calculateAccountBalance(1L);

        // Then
        assertThat(balance).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(transactionRepository, times(1)).calculateCurrentBalance(1L);
    }

    @Test
    @DisplayName("Should create deposit transaction using convenience method")
    void testCreateDepositTransaction_Success() {
        // Given
        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountService.saveAccount(any(Account.class))).thenReturn(testAccount);

        // When
        TransactionResponse response = transactionService.createDepositTransaction(
                1L, new BigDecimal("500.00"), "Deposit via convenience method");

        // Then
        assertThat(response).isNotNull();
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should create withdrawal transaction using convenience method")
    void testCreateWithdrawalTransaction_Success() {
        // Given
        Transaction withdrawalTransaction = Transaction.builder()
                .id(2L)
                .account(testAccount)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("300.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("700.00"))
                .description("Withdrawal via convenience method")
                .referenceNumber("TXN789012")
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(withdrawalTransaction);
        when(accountService.saveAccount(any(Account.class))).thenReturn(testAccount);

        // When
        TransactionResponse response = transactionService.createWithdrawalTransaction(
                1L, new BigDecimal("300.00"), "Withdrawal via convenience method");

        // Then
        assertThat(response).isNotNull();
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should process transfer between accounts successfully")
    void testProcessTransfer_Success() {
        // Given
        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC789012")
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(accountService.findAccountEntityById(2L)).thenReturn(toAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountService.saveAccount(any(Account.class))).thenReturn(testAccount);

        // When
        transactionService.processTransfer(1L, 2L, new BigDecimal("200.00"), "Transfer test");

        // Then
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // withdrawal + deposit
        verify(accountService, times(2)).saveAccount(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when transferring to same account")
    void testProcessTransfer_SameAccount_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> transactionService.processTransfer(
                1L, 1L, new BigDecimal("200.00"), "Transfer test"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Cannot transfer to the same account");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when transfer amount is zero or negative")
    void testProcessTransfer_InvalidAmount_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> transactionService.processTransfer(
                1L, 2L, BigDecimal.ZERO, "Transfer test"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer amount must be greater than zero");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when either account is inactive during transfer")
    void testProcessTransfer_InactiveAccount_ThrowsException() {
        // Given
        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC789012")
                .status(AccountStatus.INACTIVE)
                .build();

        when(accountService.findAccountEntityById(1L)).thenReturn(testAccount);
        when(accountService.findAccountEntityById(2L)).thenReturn(toAccount);

        // When & Then
        assertThatThrownBy(() -> transactionService.processTransfer(
                1L, 2L, new BigDecimal("200.00"), "Transfer test"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Both accounts must be active for transfers");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
