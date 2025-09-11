package com.portfolio.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.enums.AccountStatus;
import com.portfolio.demo.enums.AccountType;
import com.portfolio.demo.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AccountController.
 * Tests REST endpoints with real database using TestContainers.
 */
public class AccountControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    public void testCreateAccount_Success() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName", is("John Doe")))
                .andExpect(jsonPath("$.customerEmail", is("john.doe@example.com")))
                .andExpect(jsonPath("$.accountType", is("CHECKING")))
                .andExpect(jsonPath("$.balance", is(1000.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.accountNumber", notNullValue()))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    public void testCreateAccount_ValidationFailure() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("") // Invalid: empty name
                .customerEmail("invalid-email") // Invalid: bad email format
                .accountType(null) // Invalid: null account type
                .initialBalance(new BigDecimal("-100.00")) // Invalid: negative balance
                .build();

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @Transactional
    public void testGetAccountById_Success() throws Exception {
        // Create an account first
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("500.00"))
                .build();

        String createResponse = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountResponse createdAccount = objectMapper.readValue(createResponse, AccountResponse.class);

        // Test getting the account by ID
        mockMvc.perform(get("/api/accounts/{id}", createdAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdAccount.getId().intValue())))
                .andExpect(jsonPath("$.customerName", is("Jane Smith")))
                .andExpect(jsonPath("$.customerEmail", is("jane.smith@example.com")))
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    public void testGetAccountById_NotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Account not found")));
    }

    @Test
    @Transactional
    public void testGetAccountByAccountNumber_Success() throws Exception {
        // Create an account first
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("Bob Johnson")
                .customerEmail("bob.johnson@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("750.00"))
                .build();

        String createResponse = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountResponse createdAccount = objectMapper.readValue(createResponse, AccountResponse.class);

        // Test getting the account by account number
        mockMvc.perform(get("/api/accounts/account-number/{accountNumber}", createdAccount.getAccountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is(createdAccount.getAccountNumber())))
                .andExpect(jsonPath("$.customerName", is("Bob Johnson")));
    }

    @Test
    @Transactional
    public void testGetAllAccounts_Success() throws Exception {
        // Create multiple accounts
        CreateAccountRequest request1 = CreateAccountRequest.builder()
                .customerName("Alice Brown")
                .customerEmail("alice.brown@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        CreateAccountRequest request2 = CreateAccountRequest.builder()
                .customerName("Charlie Davis")
                .customerEmail("charlie.davis@example.com")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("2000.00"))
                .build();

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Test getting all accounts
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerName", anyOf(is("Alice Brown"), is("Charlie Davis"))))
                .andExpect(jsonPath("$[1].customerName", anyOf(is("Alice Brown"), is("Charlie Davis"))));
    }

    @Test
    @Transactional
    public void testGetActiveAccounts_Success() throws Exception {
        // Create an account
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("Diana Wilson")
                .customerEmail("diana.wilson@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("300.00"))
                .build();

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Test getting active accounts
        mockMvc.perform(get("/api/accounts?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[0].customerName", is("Diana Wilson")));
    }

    @Test
    @Transactional
    public void testUpdateAccountBalance_Success() throws Exception {
        // Create an account first
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("Frank Miller")
                .customerEmail("frank.miller@example.com")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        String createResponse = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountResponse createdAccount = objectMapper.readValue(createResponse, AccountResponse.class);

        // Test updating the balance
        mockMvc.perform(put("/api/accounts/{id}/balance", createdAccount.getId())
                .param("balance", "1500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1500.00)));
    }

    @Test
    @Transactional
    public void testDeactivateAccount_Success() throws Exception {
        // Create an account with zero balance for deactivation
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("Grace Lee")
                .customerEmail("grace.lee@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(BigDecimal.ZERO)
                .build();

        String createResponse = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountResponse createdAccount = objectMapper.readValue(createResponse, AccountResponse.class);

        // Test deactivating the account
        mockMvc.perform(put("/api/accounts/{id}/deactivate", createdAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @Transactional
    public void testReactivateAccount_Success() throws Exception {
        // Create an account with zero balance
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("Henry Taylor")
                .customerEmail("henry.taylor@example.com")
                .accountType(AccountType.SAVINGS)
                .initialBalance(BigDecimal.ZERO)
                .build();

        String createResponse = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountResponse createdAccount = objectMapper.readValue(createResponse, AccountResponse.class);

        // First deactivate the account
        mockMvc.perform(put("/api/accounts/{id}/deactivate", createdAccount.getId()))
                .andExpect(status().isOk());

        // Then reactivate it
        mockMvc.perform(put("/api/accounts/{id}/reactivate", createdAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}
