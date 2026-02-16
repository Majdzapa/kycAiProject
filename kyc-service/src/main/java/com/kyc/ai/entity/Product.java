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
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, name = "product_type", columnDefinition = "product_type")
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "risk_level")
    private RiskLevel baseRiskLevel;

    @Column(nullable = false)
    private Integer riskScore; // e.g., 10, 50, 80

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ProductType {
        SAVINGS_ACCOUNT,
        SALARY_ACCOUNT,
        INVESTMENT_ACCOUNT,
        BROKERAGE,
        CRYPTO_TRADING,
        CRYPTO_CUSTODY,
        PRIVATE_BANKING,
        CORRESPONDENT_BANKING,
        TRADE_FINANCE,
        INTERNATIONAL_WIRE,
        FOREIGN_EXCHANGE,
        PRECIOUS_METALS_ACCOUNT,
        CASH_MANAGEMENT,
        BUSINESS_LENDING,
        PREPAID_CARD,
        LOAN,
        CREDIT_CARD
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
