package com.portfolio.demo.controller;

import com.portfolio.demo.dto.request.CreateTransactionRequest;
import com.portfolio.demo.dto.request.TransferRequest;
import com.portfolio.demo.dto.response.TransactionResponse;
import com.portfolio.demo.enums.TransactionType;
import com.portfolio.demo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "Operations related to financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a new transaction", description = "Creates a new deposit or withdrawal transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        log.info("REST request to create {} transaction for account ID: {} with amount: {}", 
                request.getTransactionType(), request.getAccountId(), request.getAmount());
        
        TransactionResponse response = transactionService.createTransaction(request);
        
        log.info("Successfully created transaction with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves transaction details by transaction ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable Long id) {
        log.debug("REST request to get transaction with ID: {}", id);
        
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account ID", description = "Retrieves all transactions for a specific account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long accountId,
            @Parameter(description = "Transaction type filter")
            @RequestParam(value = "type", required = false) TransactionType transactionType) {
        log.debug("REST request to get transactions for account ID: {} with type filter: {}", accountId, transactionType);
        
        List<TransactionResponse> response;
        
        if (transactionType != null) {
            response = transactionService.getTransactionsByAccountIdAndType(accountId, transactionType);
        } else {
            response = transactionService.getTransactionsByAccountId(accountId);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/paginated")
    @Operation(summary = "Get paginated transactions by account ID", description = "Retrieves paginated transactions for a specific account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByAccountIdPaginated(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long accountId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.debug("REST request to get paginated transactions for account ID: {} (page: {}, size: {})", accountId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> response = transactionService.getTransactionsByAccountId(accountId, pageable);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent transactions", description = "Retrieves most recent transactions across all accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recent transactions retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            @Parameter(description = "Number of transactions to retrieve")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.debug("REST request to get {} most recent transactions", limit);
        
        // Validate limit to prevent excessive data retrieval
        if (limit > 100) {
            limit = 100;
        }
        
        List<TransactionResponse> response = transactionService.getRecentTransactions(limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Create a deposit transaction", description = "Creates a deposit transaction for the specified account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Deposit transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> createDeposit(
            @Parameter(description = "Account ID", required = true)
            @RequestParam Long accountId,
            @Parameter(description = "Deposit amount", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Transaction description")
            @RequestParam(required = false) String description) {
        log.info("REST request to create deposit for account ID: {} with amount: {}", accountId, amount);
        
        TransactionResponse response = transactionService.createDepositTransaction(accountId, amount, description);
        
        log.info("Successfully created deposit transaction with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdrawal")
    @Operation(summary = "Create a withdrawal transaction", description = "Creates a withdrawal transaction for the specified account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Withdrawal transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> createWithdrawal(
            @Parameter(description = "Account ID", required = true)
            @RequestParam Long accountId,
            @Parameter(description = "Withdrawal amount", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Transaction description")
            @RequestParam(required = false) String description) {
        log.info("REST request to create withdrawal for account ID: {} with amount: {}", accountId, amount);
        
        TransactionResponse response = transactionService.createWithdrawalTransaction(accountId, amount, description);
        
        log.info("Successfully created withdrawal transaction with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts", description = "Transfers funds from one account to another")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "One or both accounts not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> processTransfer(
            @Valid @RequestBody TransferRequest request) {
        log.info("REST request to transfer {} from account ID: {} to account ID: {}", 
                request.getAmount(), request.getFromAccountId(), request.getToAccountId());
        
        transactionService.processTransfer(
                request.getFromAccountId(), 
                request.getToAccountId(), 
                request.getAmount(), 
                request.getDescription()
        );
        
        log.info("Successfully processed transfer from account ID: {} to account ID: {}", 
                request.getFromAccountId(), request.getToAccountId());
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Transfer completed successfully"
        ));
    }

    @GetMapping("/account/{accountId}/balance")
    @Operation(summary = "Calculate account balance", description = "Calculates current account balance based on transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance calculated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, BigDecimal>> calculateAccountBalance(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long accountId) {
        log.debug("REST request to calculate balance for account ID: {}", accountId);
        
        BigDecimal balance = transactionService.calculateAccountBalance(accountId);
        
        return ResponseEntity.ok(Map.of("balance", balance));
    }
}
