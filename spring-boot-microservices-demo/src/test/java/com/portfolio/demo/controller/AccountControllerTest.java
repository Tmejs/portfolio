package com.portfolio.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.AccountType;
import com.portfolio.demo.exception.AccountNotFoundException;
import com.portfolio.demo.exception.InvalidAccountOperationException;
import com.portfolio.demo.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AccountController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 */
@WebMvcTest(AccountController.class)
@DisplayName("AccountController Unit Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private AccountResponse accountResponse;
    private CreateAccountRequest createAccountRequest;

    @BeforeEach
    void setUp() {
        accountResponse = AccountResponse.builder()
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
    @DisplayName("POST /api/accounts - Should create account successfully")
    void testCreateAccount_Success() throws Exception {
        // Given
        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenReturn(accountResponse);

        // When & Then
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.customerName", is("John Doe")))
                .andExpect(jsonPath("$.customerEmail", is("john.doe@example.com")))
                .andExpect(jsonPath("$.accountType", is("CHECKING")))
                .andExpect(jsonPath("$.balance", is(1000.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.accountNumber", is("ACC123456")));

        verify(accountService, times(1)).createAccount(any(CreateAccountRequest.class));
    }

    @Test
    @DisplayName("POST /api/accounts - Should return 400 for invalid request")
    void testCreateAccount_InvalidRequest() throws Exception {
        // Given - invalid request with empty customer name
        createAccountRequest.setCustomerName("");

        // When & Then
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(any(CreateAccountRequest.class));
    }

    @Test
    @DisplayName("GET /api/accounts/{id} - Should retrieve account by ID")
    void testGetAccountById_Success() throws Exception {
        // Given
        when(accountService.getAccountById(1L)).thenReturn(accountResponse);

        // When & Then
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.customerName", is("John Doe")))
                .andExpect(jsonPath("$.accountNumber", is("ACC123456")));

        verify(accountService, times(1)).getAccountById(1L);
    }

    @Test
    @DisplayName("GET /api/accounts/{id} - Should return 404 when account not found")
    void testGetAccountById_NotFound() throws Exception {
        // Given
        when(accountService.getAccountById(999L))
                .thenThrow(new AccountNotFoundException("Account not found with ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/accounts/999"))
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).getAccountById(999L);
    }

    @Test
    @DisplayName("GET /api/accounts/account-number/{accountNumber} - Should retrieve account by account number")
    void testGetAccountByAccountNumber_Success() throws Exception {
        // Given
        when(accountService.getAccountByAccountNumber("ACC123456")).thenReturn(accountResponse);

        // When & Then
        mockMvc.perform(get("/api/accounts/account-number/ACC123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is("ACC123456")))
                .andExpect(jsonPath("$.customerName", is("John Doe")));

        verify(accountService, times(1)).getAccountByAccountNumber("ACC123456");
    }

    @Test
    @DisplayName("GET /api/accounts - Should retrieve all accounts")
    void testGetAllAccounts_Success() throws Exception {
        // Given
        AccountResponse account2 = AccountResponse.builder()
                .id(2L)
                .accountNumber("ACC789012")
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        List<AccountResponse> accounts = Arrays.asList(accountResponse, account2);
        when(accountService.getAllAccounts()).thenReturn(accounts);

        // When & Then
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerName", is("John Doe")))
                .andExpect(jsonPath("$[1].customerName", is("Jane Smith")));

        verify(accountService, times(1)).getAllAccounts();
    }

    @Test
    @DisplayName("GET /api/accounts?active=true - Should retrieve only active accounts")
    void testGetAllAccounts_ActiveOnly() throws Exception {
        // Given
        when(accountService.getActiveAccounts()).thenReturn(Arrays.asList(accountResponse));

        // When & Then
        mockMvc.perform(get("/api/accounts")
                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));

        verify(accountService, times(1)).getActiveAccounts();
        verify(accountService, never()).getAllAccounts();
    }

    @Test
    @DisplayName("GET /api/accounts?customerName=John - Should retrieve accounts by customer name")
    void testGetAllAccounts_ByCustomerName() throws Exception {
        // Given
        when(accountService.getAccountsByCustomerName("John")).thenReturn(Arrays.asList(accountResponse));

        // When & Then
        mockMvc.perform(get("/api/accounts")
                .param("customerName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName", containsString("John")));

        verify(accountService, times(1)).getAccountsByCustomerName("John");
        verify(accountService, never()).getAllAccounts();
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/balance - Should update account balance")
    void testUpdateAccountBalance_Success() throws Exception {
        // Given
        BigDecimal newBalance = new BigDecimal("1500.00");
        AccountResponse updatedAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("ACC123456")
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .accountType(AccountType.CHECKING)
                .balance(newBalance)
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountService.updateAccountBalance(eq(1L), eq(newBalance)))
                .thenReturn(updatedAccount);

        // When & Then
        mockMvc.perform(put("/api/accounts/1/balance")
                .param("balance", "1500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1500.00)));

        verify(accountService, times(1)).updateAccountBalance(eq(1L), eq(newBalance));
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/balance - Should return 400 for negative balance")
    void testUpdateAccountBalance_NegativeBalance() throws Exception {
        // Given
        when(accountService.updateAccountBalance(eq(1L), any(BigDecimal.class)))
                .thenThrow(new InvalidAccountOperationException("Account balance cannot be negative"));

        // When & Then
        mockMvc.perform(put("/api/accounts/1/balance")
                .param("balance", "-100.00"))
                .andExpect(status().isBadRequest());

        verify(accountService, times(1)).updateAccountBalance(eq(1L), any(BigDecimal.class));
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/deactivate - Should deactivate account")
    void testDeactivateAccount_Success() throws Exception {
        // Given
        AccountResponse deactivatedAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("ACC123456")
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .accountType(AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .status(AccountStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountService.deactivateAccount(1L)).thenReturn(deactivatedAccount);

        // When & Then
        mockMvc.perform(put("/api/accounts/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));

        verify(accountService, times(1)).deactivateAccount(1L);
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/deactivate - Should return 400 when account has positive balance")
    void testDeactivateAccount_PositiveBalance() throws Exception {
        // Given
        when(accountService.deactivateAccount(1L))
                .thenThrow(new InvalidAccountOperationException("Cannot deactivate account with positive balance"));

        // When & Then
        mockMvc.perform(put("/api/accounts/1/deactivate"))
                .andExpect(status().isBadRequest());

        verify(accountService, times(1)).deactivateAccount(1L);
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/reactivate - Should reactivate account")
    void testReactivateAccount_Success() throws Exception {
        // Given
        when(accountService.reactivateAccount(1L)).thenReturn(accountResponse);

        // When & Then
        mockMvc.perform(put("/api/accounts/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(accountService, times(1)).reactivateAccount(1L);
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/reactivate - Should return 400 when account is already active")
    void testReactivateAccount_AlreadyActive() throws Exception {
        // Given
        when(accountService.reactivateAccount(1L))
                .thenThrow(new InvalidAccountOperationException("Account is already active"));

        // When & Then
        mockMvc.perform(put("/api/accounts/1/reactivate"))
                .andExpect(status().isBadRequest());

        verify(accountService, times(1)).reactivateAccount(1L);
    }
}
