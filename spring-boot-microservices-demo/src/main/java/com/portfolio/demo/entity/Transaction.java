package com.portfolio.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing a banking transaction
 * Uses Lombok to reduce boilerplate code
 */
@Entity
@Table(name = "transactions", schema = "banking", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_transaction_type", columnList = "transactionType"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_reference_number", columnList = "referenceNumber", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"account"})
@ToString(exclude = {"account"})
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reference_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Reference number is required")
    @Size(min = 10, max = 50, message = "Reference number must be between 10 and 50 characters")
    private String referenceNumber;
    
    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Column(name = "balance_after", precision = 19, scale = 2, nullable = false)
    @NotNull(message = "Balance after transaction is required")
    private BigDecimal balanceAfter;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private Account account;
    
}
