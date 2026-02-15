package com.kyc.ai.repository;

import com.kyc.ai.entity.KycDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

       List<KycDocument> findByCustomerId(String customerId);

       Optional<KycDocument> findByCustomerIdAndDocumentType(String customerId, KycDocument.DocumentType documentType);

       List<KycDocument> findByVerificationStatus(KycDocument.VerificationStatus status);

       List<KycDocument> findByRiskLevel(KycDocument.RiskLevel riskLevel);

       @Query("SELECT d FROM KycDocument d WHERE d.dataRetentionUntil < :now AND d.anonymized = false")
       List<KycDocument> findExpiredDocuments(@Param("now") LocalDateTime now);

       @Query("SELECT d FROM KycDocument d WHERE d.customerId = :customerId ORDER BY d.createdAt DESC")
       Page<KycDocument> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") String customerId,
                     Pageable pageable);

       @Query("SELECT COUNT(d) FROM KycDocument d WHERE d.verificationStatus = :status")
       long countByVerificationStatus(@Param("status") KycDocument.VerificationStatus status);

       @Modifying
       @Query("UPDATE KycDocument d SET d.anonymized = true, d.text = '[ANONYMIZED]', " +
                     "d.extractedData = null WHERE d.id = :id")
       void anonymizeDocument(@Param("id") UUID id);

       @Modifying
       @Query("DELETE FROM KycDocument d WHERE d.dataRetentionUntil < :now")
       int deleteExpiredDocuments(@Param("now") LocalDateTime now);

       @Query("SELECT d FROM KycDocument d WHERE d.confidenceScore < :threshold AND d.verificationStatus = 'PENDING'")
       List<KycDocument> findLowConfidenceDocuments(@Param("threshold") Double threshold);

       boolean existsByCustomerIdAndDocumentTypeAndVerificationStatus(
                     String customerId,
                     KycDocument.DocumentType documentType,
                     KycDocument.VerificationStatus status);
}
