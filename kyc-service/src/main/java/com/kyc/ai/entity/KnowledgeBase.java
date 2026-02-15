package com.kyc.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_base")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "embedding_id")
    private UUID id;

    @Column(name = "category", length = 100)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "version", length = 20)
    private String version;

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ingested_by", length = 255)
    private String ingestedBy;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public enum Category {
        REGULATION,
        PROCEDURE,
        GUIDELINE,
        FAQ,
        POLICY
    }
}
