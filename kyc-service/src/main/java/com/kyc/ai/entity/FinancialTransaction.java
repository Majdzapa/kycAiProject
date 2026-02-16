package com.kyc.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_type")
    private TransactionType type;

    @Column(name = "source_country", length = 2)
    private String sourceCountry;

    @Column(name = "destination_country", length = 2)
    private String destinationCountry;

    private String counterpartyName;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public enum TransactionType {
        DEPOSIT(true),
        WITHDRAWAL(false),
        TRANSFER(false),
        CRYPTO_PURCHASE(false),
        CRYPTO_SALE(true),
        PAYMENT(false),
        CASH_DEPOSIT(true),
        CASH_WITHDRAWAL(false);

        private final boolean credit;

        TransactionType(boolean credit) {
            this.credit = credit;
        }

        public boolean isCredit() {
            return credit;
        }

        public boolean isDebit() {
            return !credit;
        }
    }
}
