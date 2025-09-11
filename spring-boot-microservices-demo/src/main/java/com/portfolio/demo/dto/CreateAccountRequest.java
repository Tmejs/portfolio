package com.portfolio.demo.dto;

import com.portfolio.demo.entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new bank account
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {
    
    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String customerName;
    
    @NotBlank(message = "Customer email is required")
    @Email(message = "Valid email address is required")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String customerEmail;
    
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    @PositiveOrZero(message = "Initial balance cannot be negative")
    private BigDecimal initialBalance = BigDecimal.ZERO;
    
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO code)")
    private String currency = "USD";
}
