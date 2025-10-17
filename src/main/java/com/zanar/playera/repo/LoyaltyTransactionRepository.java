package com.zanar.playera.repo;

import com.zanar.playera.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

  List<LoyaltyTransaction> findByCustomerUserIdOrderByTransactionDateDesc(Long customerId);

  List<LoyaltyTransaction> findByCustomerUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
      Long customerId, LocalDateTime startDate, LocalDateTime endDate);

  @Query("SELECT lt FROM LoyaltyTransaction lt WHERE lt.customer.userId = :customerId " +
      "ORDER BY lt.transactionDate DESC")
  List<LoyaltyTransaction> findRecentTransactionsByCustomer(@Param("customerId") Long customerId);

  @Query("SELECT lt FROM LoyaltyTransaction lt WHERE lt.customer.userId = :customerId " +
      "AND lt.transactionType = :transactionType " +
      "ORDER BY lt.transactionDate DESC")
  List<LoyaltyTransaction> findByCustomerAndType(@Param("customerId") Long customerId,
      @Param("transactionType") LoyaltyTransaction.TransactionType transactionType);
}
