package com.kyc.ai.repository;

import com.kyc.ai.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {

    List<FinancialTransaction> findByCustomerId(String customerId);

    @Query("SELECT t FROM FinancialTransaction t WHERE t.customer.id = :customerId AND t.timestamp >= :since")
    List<FinancialTransaction> findRecentTransactions(@Param("customerId") String customerId,
            @Param("since") LocalDateTime since);
}
