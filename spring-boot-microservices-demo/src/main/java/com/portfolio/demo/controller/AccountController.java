package com.portfolio.demo.controller;

import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "Operations related to bank accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new account", description = "Creates a new bank account with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("REST request to create account for customer: {}", request.getCustomerName());
        
        AccountResponse response = accountService.createAccount(request);
        
        log.info("Successfully created account with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID", description = "Retrieves account details by account ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long id) {
        log.debug("REST request to get account with ID: {}", id);
        
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account-number/{accountNumber}")
    @Operation(summary = "Get account by account number", description = "Retrieves account details by account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber) {
        log.debug("REST request to get account with number: {}", accountNumber);
        
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieves all bank accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @Parameter(description = "Filter by active status")
            @RequestParam(value = "active", required = false) Boolean active,
            @Parameter(description = "Filter by customer name")
            @RequestParam(value = "customerName", required = false) String customerName) {
        log.debug("REST request to get accounts with filters - active: {}, customerName: {}", active, customerName);
        
        List<AccountResponse> response;
        
        if (customerName != null && !customerName.trim().isEmpty()) {
            response = accountService.getAccountsByCustomerName(customerName.trim());
        } else if (active != null && active) {
            response = accountService.getActiveAccounts();
        } else {
            response = accountService.getAllAccounts();
        }
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/balance")
    @Operation(summary = "Update account balance", description = "Updates the balance of an existing account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid balance amount"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AccountResponse> updateAccountBalance(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "New balance amount", required = true)
            @RequestParam BigDecimal balance) {
        log.info("REST request to update balance for account ID: {} to: {}", id, balance);
        
        AccountResponse response = accountService.updateAccountBalance(id, balance);
        
        log.info("Successfully updated balance for account ID: {}", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate account", description = "Deactivates an existing account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot deactivate account with positive balance"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AccountResponse> deactivateAccount(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to deactivate account with ID: {}", id);
        
        AccountResponse response = accountService.deactivateAccount(id);
        
        log.info("Successfully deactivated account with ID: {}", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate account", description = "Reactivates a previously deactivated account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account reactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Account is already active"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AccountResponse> reactivateAccount(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to reactivate account with ID: {}", id);
        
        AccountResponse response = accountService.reactivateAccount(id);
        
        log.info("Successfully reactivated account with ID: {}", id);
        return ResponseEntity.ok(response);
    }
}
