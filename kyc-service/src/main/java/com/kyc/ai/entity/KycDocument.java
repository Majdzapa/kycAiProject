package com.kyc.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "embedding_id")
    private UUID id;

    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;

    @Column(name = "document_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "verification_status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "extracted_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String extractedData;

    @Column(name = "risk_level", length = 50)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // GDPR compliance fields
    @Column(name = "data_retention_until")
    private LocalDateTime dataRetentionUntil;

    @Column(name = "processing_legal_basis", length = 50)
    @Enumerated(EnumType.STRING)
    private LegalBasis processingLegalBasis;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;

    @Column(name = "anonymized")
    @Builder.Default
    private Boolean anonymized = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private String processedBy;

    public enum DocumentType {
        ID_CARD,
        PASSPORT,
        DRIVERS_LICENSE,
        PROOF_OF_ADDRESS,
        UTILITY_BILL,
        BANK_STATEMENT
    }

    public enum VerificationStatus {
        PENDING,
        IN_PROGRESS,
        VERIFIED,
        REJECTED,
        NEEDS_REVIEW,
        EXPIRED
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum LegalBasis {
        CONSENT,
        LEGAL_OBLIGATION,
        CONTRACT,
        LEGITIMATE_INTEREST,
        VITAL_INTEREST,
        PUBLIC_TASK
    }
}
