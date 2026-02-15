package com.kyc.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", length = 255)
    private String customerId;

    @Column(name = "action", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "legal_basis", length = 50)
    @Enumerated(EnumType.STRING)
    private LegalBasis legalBasis;

    @Column(name = "data_categories", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] dataCategories;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String details;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public enum AuditAction {
        VIEW,
        CREATE,
        UPDATE,
        DELETE,
        PROCESS,
        EXPORT,
        IMPORT,
        LOGIN,
        LOGOUT,
        CONSENT_GIVEN,
        CONSENT_REVOKED,
        DATA_EXPORT,
        DATA_DELETION,
        AUTO_PURGE,
        CHAT_INTERACTION, ACCESS_DENIED
    }

    public enum LegalBasis {
        CONSENT,
        LEGAL_OBLIGATION,
        CONTRACT,
        LEGITIMATE_INTEREST,
        GDPR_ARTICLE_17,
        SYSTEM
    }
}
