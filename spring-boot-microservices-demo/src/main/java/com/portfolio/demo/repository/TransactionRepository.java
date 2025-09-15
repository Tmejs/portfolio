package com.portfolio.demo.repository;

import com.portfolio.demo.entity.Transaction;
import com.portfolio.demo.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transaction entity
 * Provides CRUD operations and custom queries for transaction operations
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find transaction by reference number
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    
    /**
     * Find transactions by account ID
     */
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    
    /**
     * Find transactions by account ID with pagination
     */
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    
    /**
     * Find transactions by account number
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);
    
    /**
     * Find transactions by account number with pagination
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    /**
     * Find transactions by type
     */
    List<Transaction> findByTransactionType(TransactionType transactionType);
    
    /**
     * Find transactions within date range for specific account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountAndDateRange(@Param("accountId") Long accountId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find transactions with amount greater than specified value
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount > :minAmount ORDER BY t.amount DESC")
    List<Transaction> findTransactionsAboveAmount(@Param("minAmount") BigDecimal minAmount);
    
    /**
     * Get total amount by transaction type for specific account
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.id = :accountId AND t.transactionType = :transactionType")
    BigDecimal getTotalAmountByTypeAndAccount(@Param("accountId") Long accountId, 
                                            @Param("transactionType") TransactionType transactionType);
    
    /**
     * Count transactions by type for specific account
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId AND t.transactionType = :transactionType")
    long countByAccountAndType(@Param("accountId") Long accountId, 
                              @Param("transactionType") TransactionType transactionType);
    
    /**
     * Find recent transactions (last N days) for analytics
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions(@Param("since") LocalDateTime since);
    
    /**
     * Get daily transaction summary
     */
    @Query("SELECT DATE(t.createdAt) as date, t.transactionType, COUNT(t) as count, SUM(t.amount) as total " +
           "FROM Transaction t WHERE t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "GROUP BY DATE(t.createdAt), t.transactionType ORDER BY date DESC")
    List<Object[]> getDailyTransactionSummary(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Check if reference number exists
     */
    boolean existsByReferenceNumber(String referenceNumber);
    
    /**
     * Find transactions by account ID and transaction type
     */
    List<Transaction> findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(Long accountId, TransactionType transactionType);
    
    /**
     * Find recent transactions with limit
     */
    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC LIMIT :limit")
    List<Transaction> findRecentTransactions(@Param("limit") int limit);
    
    /**
     * Calculate current balance for an account based on transactions
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 'DEPOSIT' THEN t.amount ELSE -t.amount END), 0) FROM Transaction t WHERE t.account.id = :accountId")
    BigDecimal calculateCurrentBalance(@Param("accountId") Long accountId);
}
