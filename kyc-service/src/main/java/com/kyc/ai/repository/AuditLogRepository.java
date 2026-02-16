package com.kyc.ai.repository;

import com.kyc.ai.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

        Page<AuditLog> findByCustomerIdOrderByPerformedAtDesc(String customerId, Pageable pageable);

        Page<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy, Pageable pageable);

        @Query("SELECT a FROM AuditLog a WHERE a.performedAt BETWEEN :start AND :end ORDER BY a.performedAt DESC")
        Page<AuditLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                        Pageable pageable);

        @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.performedAt DESC")
        Page<AuditLog> findByAction(@Param("action") AuditLog.AuditAction action, Pageable pageable);

        @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.performedAt BETWEEN :start AND :end")
        long countByActionAndDateRange(@Param("action") AuditLog.AuditAction action,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT a FROM AuditLog a WHERE a.customerId = :customerId AND a.action IN :actions ORDER BY a.performedAt DESC")
        List<AuditLog> findByCustomerIdAndActions(@Param("customerId") String customerId,
                        @Param("actions") List<AuditLog.AuditAction> actions);

        @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.performedAt DESC")
        Page<AuditLog> findFailedActions(Pageable pageable);

        @Query("SELECT a.dataCategories, COUNT(a) FROM AuditLog a WHERE a.performedAt BETWEEN :start AND :end GROUP BY a.dataCategories")
        List<Object[]> countAccessByDataCategory(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT a FROM AuditLog a WHERE a.customerId = :customerId AND a.action = 'DATA_EXPORT' ORDER BY a.performedAt DESC")
        List<AuditLog> findDataExportsByCustomer(@Param("customerId") String customerId);
}
