package com.portfolio.demo.repository;

import com.portfolio.demo.entity.Account;
import com.portfolio.demo.entity.AccountStatus;
import com.portfolio.demo.entity.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity
 * Provides CRUD operations and custom queries for banking operations
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * Find account by account number
     */
    Optional<Account> findByAccountNumber(String accountNumber);
    
    /**
     * Find accounts by customer email
     */
    List<Account> findByCustomerEmail(String customerEmail);
    
    /**
     * Find accounts by customer name (case-insensitive)
     */
    @Query("SELECT a FROM Account a WHERE LOWER(a.customerName) LIKE LOWER(CONCAT('%', :customerName, '%'))")
    List<Account> findByCustomerNameContainingIgnoreCase(@Param("customerName") String customerName);
    
    /**
     * Find accounts by account type
     */
    List<Account> findByAccountType(AccountType accountType);
    
    /**
     * Find accounts by status
     */
    List<Account> findByStatus(AccountStatus status);
    
    /**
     * Find active accounts with pageable support
     */
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);
    
    /**
     * Find accounts with balance greater than specified amount
     */
    @Query("SELECT a FROM Account a WHERE a.balance > :minBalance AND a.status = :status")
    List<Account> findAccountsWithMinBalance(@Param("minBalance") BigDecimal minBalance, 
                                           @Param("status") AccountStatus status);
    
    /**
     * Get total balance by account type
     */
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.accountType = :accountType AND a.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByAccountType(@Param("accountType") AccountType accountType);
    
    /**
     * Count accounts by status
     */
    long countByStatus(AccountStatus status);
    
    /**
     * Check if account number exists
     */
    boolean existsByAccountNumber(String accountNumber);
    
    /**
     * Find accounts created within date range
     */
    @Query("SELECT a FROM Account a WHERE a.createdAt >= :startDate AND a.createdAt <= :endDate")
    List<Account> findAccountsCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate);
}
