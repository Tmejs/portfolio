package com.portfolio.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.demo.dto.request.CreateTransactionRequest;
import com.portfolio.demo.dto.request.TransferRequest;
import com.portfolio.demo.dto.response.TransactionResponse;
import com.portfolio.demo.enums.TransactionType;
import com.portfolio.demo.exception.InsufficientFundsException;
import com.portfolio.demo.exception.InvalidTransactionException;
import com.portfolio.demo.exception.TransactionNotFoundException;
import com.portfolio.demo.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
 * Unit tests for TransactionController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 */
@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController Unit Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionResponse transactionResponse;
    private CreateTransactionRequest createTransactionRequest;

    @BeforeEach
    void setUp() {
        transactionResponse = TransactionResponse.builder()
                .id(1L)
                .accountId(1L)
                .accountNumber("ACC123456")
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .description("Test deposit")
                .referenceNumber("TXN123456")
                .createdAt(LocalDateTime.now())
                .build();

        createTransactionRequest = CreateTransactionRequest.builder()
                .accountId(1L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .description("Test deposit")
                .build();
    }

    @Test
    @DisplayName("POST /api/transactions - Should create transaction successfully")
    void testCreateTransaction_Success() throws Exception {
        // Given
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(transactionResponse);

        // When & Then
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.accountId", is(1)))
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", is(500.00)))
                .andExpect(jsonPath("$.balanceBefore", is(1000.00)))
                .andExpect(jsonPath("$.balanceAfter", is(1500.00)))
                .andExpect(jsonPath("$.referenceNumber", is("TXN123456")));

        verify(transactionService, times(1)).createTransaction(any(CreateTransactionRequest.class));
    }

    @Test
    @DisplayName("POST /api/transactions - Should return 400 for invalid request")
    void testCreateTransaction_InvalidRequest() throws Exception {
        // Given - invalid request with null transaction type
        createTransactionRequest.setTransactionType(null);

        // When & Then
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).createTransaction(any(CreateTransactionRequest.class));
    }

    @Test
    @DisplayName("POST /api/transactions - Should return 400 for insufficient funds")
    void testCreateTransaction_InsufficientFunds() throws Exception {
        // Given
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        // When & Then
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, times(1)).createTransaction(any(CreateTransactionRequest.class));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} - Should retrieve transaction by ID")
    void testGetTransactionById_Success() throws Exception {
        // Given
        when(transactionService.getTransactionById(1L)).thenReturn(transactionResponse);

        // When & Then
        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.referenceNumber", is("TXN123456")));

        verify(transactionService, times(1)).getTransactionById(1L);
    }

    @Test
    @DisplayName("GET /api/transactions/{id} - Should return 404 when transaction not found")
    void testGetTransactionById_NotFound() throws Exception {
        // Given
        when(transactionService.getTransactionById(999L))
                .thenThrow(new TransactionNotFoundException("Transaction not found with ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/transactions/999"))
                .andExpect(status().isNotFound());

        verify(transactionService, times(1)).getTransactionById(999L);
    }

    @Test
    @DisplayName("GET /api/transactions/account/{accountId} - Should retrieve transactions by account ID")
    void testGetTransactionsByAccountId_Success() throws Exception {
        // Given
        TransactionResponse transaction2 = TransactionResponse.builder()
                .id(2L)
                .accountId(1L)
                .accountNumber("ACC123456")
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("200.00"))
                .balanceBefore(new BigDecimal("1500.00"))
                .balanceAfter(new BigDecimal("1300.00"))
                .description("Test withdrawal")
                .referenceNumber("TXN789012")
                .createdAt(LocalDateTime.now())
                .build();

        List<TransactionResponse> transactions = Arrays.asList(transactionResponse, transaction2);
        when(transactionService.getTransactionsByAccountId(1L)).thenReturn(transactions);

        // When & Then
        mockMvc.perform(get("/api/transactions/account/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$[1].transactionType", is("WITHDRAWAL")));

        verify(transactionService, times(1)).getTransactionsByAccountId(1L);
    }

    @Test
    @DisplayName("GET /api/transactions/account/{accountId}/paginated?page=0&size=10 - Should retrieve transactions with pagination")
    void testGetTransactionsByAccountId_WithPagination() throws Exception {
        // Given
        Page<TransactionResponse> transactionPage = new PageImpl<>(
                Arrays.asList(transactionResponse),
                PageRequest.of(0, 10),
                1);

        when(transactionService.getTransactionsByAccountId(eq(1L), any(PageRequest.class)))
                .thenReturn(transactionPage);

        // When & Then
        mockMvc.perform(get("/api/transactions/account/1/paginated")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(transactionService, times(1)).getTransactionsByAccountId(eq(1L), any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /api/transactions/account/{accountId}?type=DEPOSIT - Should retrieve transactions by type")
    void testGetTransactionsByAccountIdAndType_Success() throws Exception {
        // Given
        when(transactionService.getTransactionsByAccountIdAndType(1L, TransactionType.DEPOSIT))
                .thenReturn(Arrays.asList(transactionResponse));

        // When & Then
        mockMvc.perform(get("/api/transactions/account/1")
                .param("type", "DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType", is("DEPOSIT")));

        verify(transactionService, times(1))
                .getTransactionsByAccountIdAndType(1L, TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("GET /api/transactions/recent - Should retrieve recent transactions")
    void testGetRecentTransactions_Success() throws Exception {
        // Given
        when(transactionService.getRecentTransactions(10))
                .thenReturn(Arrays.asList(transactionResponse));

        // When & Then
        mockMvc.perform(get("/api/transactions/recent")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(transactionService, times(1)).getRecentTransactions(10);
    }

    @Test
    @DisplayName("GET /api/transactions/account/{accountId}/balance - Should calculate account balance")
    void testCalculateAccountBalance_Success() throws Exception {
        // Given
        when(transactionService.calculateAccountBalance(1L))
                .thenReturn(new BigDecimal("1500.00"));

        // When & Then
        mockMvc.perform(get("/api/transactions/account/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1500.00)));

        verify(transactionService, times(1)).calculateAccountBalance(1L);
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - Should process transfer successfully")
    void testProcessTransfer_Success() throws Exception {
        // Given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("200.00"))
                .description("Transfer test")
                .build();

        doNothing().when(transactionService).processTransfer(
                eq(1L), eq(2L), eq(new BigDecimal("200.00")), eq("Transfer test"));

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transfer completed successfully")));

        verify(transactionService, times(1)).processTransfer(
                eq(1L), eq(2L), eq(new BigDecimal("200.00")), eq("Transfer test"));
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - Should return 400 for invalid transfer")
    void testProcessTransfer_InvalidTransfer() throws Exception {
        // Given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(1L) // Same account
                .amount(new BigDecimal("200.00"))
                .description("Transfer test")
                .build();

        doThrow(new InvalidTransactionException("Cannot transfer to the same account"))
                .when(transactionService).processTransfer(any(), any(), any(), any());

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, times(1)).processTransfer(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - Should return 400 for insufficient funds during transfer")
    void testProcessTransfer_InsufficientFunds() throws Exception {
        // Given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("5000.00"))
                .description("Transfer test")
                .build();

        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(transactionService).processTransfer(any(), any(), any(), any());

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, times(1)).processTransfer(any(), any(), any(), any());
    }
}
