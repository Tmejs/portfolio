package com.portfolio.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.request.CreateTransactionRequest;
import com.portfolio.demo.dto.request.TransferRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.dto.response.TransactionResponse;
import com.portfolio.demo.enums.AccountType;
import com.portfolio.demo.enums.TransactionType;
import com.portfolio.demo.repository.AccountRepository;
import com.portfolio.demo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransactionController.
 * Tests REST endpoints with real database using TestContainers.
 */
public class TransactionControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @Transactional
    public void testCreateTransaction_Deposit_Success() throws Exception {
        // Create an account first
        AccountResponse account = createTestAccount("John Doe", "john@example.com", AccountType.CHECKING, new BigDecimal("1000.00"));

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .description("Test deposit")
                .build();

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId", is(account.getId().intValue())))
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", is(500.00)))
                .andExpect(jsonPath("$.balanceBefore", is(1000.00)))
                .andExpect(jsonPath("$.balanceAfter", is(1500.00)))
                .andExpect(jsonPath("$.description", is("Test deposit")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @Transactional
    public void testCreateTransaction_Withdrawal_Success() throws Exception {
        // Create an account with sufficient balance
        AccountResponse account = createTestAccount("Jane Smith", "jane@example.com", AccountType.SAVINGS, new BigDecimal("1000.00"));

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("300.00"))
                .description("Test withdrawal")
                .build();

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId", is(account.getId().intValue())))
                .andExpect(jsonPath("$.transactionType", is("WITHDRAWAL")))
                .andExpect(jsonPath("$.amount", is(300.00)))
                .andExpect(jsonPath("$.balanceBefore", is(1000.00)))
                .andExpect(jsonPath("$.balanceAfter", is(700.00)))
                .andExpect(jsonPath("$.description", is("Test withdrawal")));
    }

    @Test
    @Transactional
    public void testCreateTransaction_InsufficientFunds_Failure() throws Exception {
        // Create an account with insufficient balance
        AccountResponse account = createTestAccount("Bob Johnson", "bob@example.com", AccountType.CHECKING, new BigDecimal("100.00"));

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("500.00"))
                .description("Test insufficient funds")
                .build();

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Insufficient Funds")))
                .andExpect(jsonPath("$.message", containsString("Insufficient funds")));
    }

    @Test
    @Transactional
    public void testCreateTransaction_ValidationFailure() throws Exception {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(null) // Invalid: null account ID
                .transactionType(null) // Invalid: null transaction type
                .amount(new BigDecimal("-100.00")) // Invalid: negative amount
                .build();

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @Transactional
    public void testGetTransactionById_Success() throws Exception {
        // Create account and transaction
        AccountResponse account = createTestAccount("Alice Brown", "alice@example.com", AccountType.SAVINGS, new BigDecimal("500.00"));
        TransactionResponse createdTransaction = createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("200.00"));

        mockMvc.perform(get("/api/transactions/{id}", createdTransaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdTransaction.getId().intValue())))
                .andExpect(jsonPath("$.accountId", is(account.getId().intValue())))
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", is(200.00)));
    }

    @Test
    public void testGetTransactionById_NotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Transaction not found")));
    }

    @Test
    @Transactional
    public void testGetTransactionsByAccountId_Success() throws Exception {
        // Create account and multiple transactions
        AccountResponse account = createTestAccount("Charlie Davis", "charlie@example.com", AccountType.CHECKING, new BigDecimal("1000.00"));
        
        createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("100.00"));
        createTestTransaction(account.getId(), TransactionType.WITHDRAWAL, new BigDecimal("50.00"));

        mockMvc.perform(get("/api/transactions/account/{accountId}", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountId", is(account.getId().intValue())))
                .andExpect(jsonPath("$[1].accountId", is(account.getId().intValue())));
    }

    @Test
    @Transactional
    public void testGetTransactionsByAccountIdAndType_Success() throws Exception {
        // Create account and transactions of different types
        AccountResponse account = createTestAccount("Diana Wilson", "diana@example.com", AccountType.SAVINGS, new BigDecimal("800.00"));
        
        createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("200.00"));
        createTestTransaction(account.getId(), TransactionType.WITHDRAWAL, new BigDecimal("100.00"));
        createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("300.00"));

        // Filter by DEPOSIT transactions only
        mockMvc.perform(get("/api/transactions/account/{accountId}?type=DEPOSIT", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$[1].transactionType", is("DEPOSIT")));
    }

    @Test
    @Transactional
    public void testCreateDeposit_Success() throws Exception {
        AccountResponse account = createTestAccount("Frank Miller", "frank@example.com", AccountType.CHECKING, new BigDecimal("500.00"));

        mockMvc.perform(post("/api/transactions/deposit")
                .param("accountId", account.getId().toString())
                .param("amount", "250.00")
                .param("description", "Test deposit via endpoint"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", is(250.00)))
                .andExpect(jsonPath("$.description", is("Test deposit via endpoint")));
    }

    @Test
    @Transactional
    public void testCreateWithdrawal_Success() throws Exception {
        AccountResponse account = createTestAccount("Grace Lee", "grace@example.com", AccountType.SAVINGS, new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/transactions/withdrawal")
                .param("accountId", account.getId().toString())
                .param("amount", "400.00")
                .param("description", "Test withdrawal via endpoint"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType", is("WITHDRAWAL")))
                .andExpect(jsonPath("$.amount", is(400.00)))
                .andExpect(jsonPath("$.description", is("Test withdrawal via endpoint")));
    }

    @Test
    @Transactional
    public void testProcessTransfer_Success() throws Exception {
        // Create two accounts
        AccountResponse fromAccount = createTestAccount("Henry Taylor", "henry@example.com", AccountType.CHECKING, new BigDecimal("1000.00"));
        AccountResponse toAccount = createTestAccount("Ivy Davis", "ivy@example.com", AccountType.SAVINGS, new BigDecimal("500.00"));

        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("300.00"))
                .description("Test transfer")
                .build();

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Transfer completed successfully")));

        // Verify both accounts have updated balances by checking transactions
        mockMvc.perform(get("/api/transactions/account/{accountId}", fromAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType", is("WITHDRAWAL")))
                .andExpect(jsonPath("$[0].amount", is(300.00)));

        mockMvc.perform(get("/api/transactions/account/{accountId}", toAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$[0].amount", is(300.00)));
    }

    @Test
    @Transactional
    public void testProcessTransfer_InsufficientFunds_Failure() throws Exception {
        // Create accounts with insufficient funds
        AccountResponse fromAccount = createTestAccount("Jack Wilson", "jack@example.com", AccountType.CHECKING, new BigDecimal("100.00"));
        AccountResponse toAccount = createTestAccount("Kate Brown", "kate@example.com", AccountType.SAVINGS, new BigDecimal("200.00"));

        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("500.00"))
                .description("Test insufficient transfer")
                .build();

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Insufficient Funds")));
    }

    @Test
    @Transactional
    public void testGetRecentTransactions_Success() throws Exception {
        // Create account and some transactions
        AccountResponse account = createTestAccount("Laura Garcia", "laura@example.com", AccountType.CHECKING, new BigDecimal("1000.00"));
        
        createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("100.00"));
        createTestTransaction(account.getId(), TransactionType.WITHDRAWAL, new BigDecimal("50.00"));
        createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("200.00"));

        mockMvc.perform(get("/api/transactions/recent?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) // Should return all 3 transactions
                .andExpect(jsonPath("$[0].accountId", is(account.getId().intValue())));
    }

    @Test
    @Transactional
    public void testCalculateAccountBalance_Success() throws Exception {
        AccountResponse account = createTestAccount("Mike Johnson", "mike@example.com", AccountType.SAVINGS, new BigDecimal("600.00"));
        
        // Create some transactions
        createTestTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("400.00"));
        createTestTransaction(account.getId(), TransactionType.WITHDRAWAL, new BigDecimal("200.00"));

        mockMvc.perform(get("/api/transactions/account/{accountId}/balance", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(800.00))); // 600 + 400 - 200 = 800
    }

    // Helper methods
    private AccountResponse createTestAccount(String name, String email, AccountType type, BigDecimal balance) throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName(name)
                .customerEmail(email)
                .accountType(type)
                .initialBalance(balance)
                .build();

        String response = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AccountResponse.class);
    }

    private TransactionResponse createTestTransaction(Long accountId, TransactionType type, BigDecimal amount) throws Exception {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .transactionType(type)
                .amount(amount)
                .description("Test transaction")
                .build();

        String response = mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, TransactionResponse.class);
    }
}
