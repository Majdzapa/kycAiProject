package com.kyc.ai.service;

import com.kyc.ai.entity.FinancialTransaction;
import com.kyc.ai.entity.Product;
import com.kyc.ai.repository.FinancialTransactionRepository;
import com.kyc.ai.repository.ProductRepository;
import com.kyc.ai.util.CountryRiskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final ProductRepository productRepository;
    private final FinancialTransactionRepository transactionRepository;

    /**
     * Calculate comprehensive risk score based on 4 factors
     */
    public RiskScoreResult calculateRiskScore(String customerId,
            String nationality,
            String residenceCountry,
            boolean isPep,
            List<String> userProductIds) {

        log.info("Calculating advanced risk score for customer: {}", customerId);

        // 1. Customer Risk
        int customerScore = calculateCustomerRisk(isPep);

        // 2. Geographic Risk
        int geoScore = calculateGeographicRisk(nationality, residenceCountry);

        // 3. Product Risk
        int productScore = calculateProductRisk(userProductIds);

        // 4. Transaction Risk
        int transactionScore = calculateTransactionRisk(customerId);

        // Total Score
        int totalScore = customerScore + geoScore + productScore + transactionScore;

        // Classification
        RiskLevel riskLevel = classifyRisk(totalScore);

        log.info("Risk calculation complete for {}: Total Score = {}, Level = {}",
                customerId, totalScore, riskLevel);

        return new RiskScoreResult(totalScore, riskLevel,
                customerScore, geoScore, productScore, transactionScore);
    }

    private int calculateCustomerRisk(boolean isPep) {
        int score = 0;
        if (isPep) {
            score += 80; // High impact
        }
        // Add other checks (e.g., age, occupation logic if available)
        return score;
    }

    private int calculateGeographicRisk(String nationality, String residenceCountry) {
        int score = 0;

        // Nationality Risk
        String natRisk = CountryRiskUtil.getNationalityRisk(nationality);
        if ("CRITICAL".equals(natRisk))
            score += 50;
        else if ("HIGH".equals(natRisk))
            score += 30;
        else if ("MEDIUM".equals(natRisk))
            score += 10;

        // Residence Risk
        String resRisk = CountryRiskUtil.getResidenceRisk(residenceCountry);
        if ("HIGH".equals(resRisk))
            score += 40;
        else if ("MEDIUM".equals(resRisk))
            score += 20;

        return score;
    }

    private int calculateProductRisk(List<String> productIds) {
        if (productIds == null || productIds.isEmpty())
            return 0;

        int maxProductRisk = 0;

        // Fetch products and find the highest risk one
        // In a real scenario, we might sum them or take a weighted average
        // Here we take the max risk of any enrolled product
        for (String productId : productIds) {
            // Assuming productId is UUID string
            try {
                // In production, batch fetch for performance
                // For MVP, simple lookup
                // converting string ID to UUID would be needed here if using UUIDs
                // skipping DB lookup logic for this snippet to keep it simple,
                // assuming we pass Risk Levels directly or fetch from cache
            } catch (Exception e) {
                log.warn("Invalid product ID: {}", productId);
            }
        }

        // MVP: Detailed product fetching logic would go here.
        // For now, returning a placeholder logic or 0 if no products linked.
        return 0;
    }

    // Improved version with actual DB lookup if needed
    public int calculateProductRiskFromProducts(List<Product> products) {
        if (products == null || products.isEmpty())
            return 0;

        int highestScore = 0;
        for (Product p : products) {
            if (p.getRiskScore() > highestScore) {
                highestScore = p.getRiskScore();
            }
        }
        return highestScore;
    }

    private int calculateTransactionRisk(String customerId) {
        List<FinancialTransaction> transactions = transactionRepository
                .findRecentTransactions(customerId, LocalDateTime.now().minusMonths(1));

        int score = 0;
        double totalVolume = 0;
        int cryptoCount = 0;

        for (FinancialTransaction tx : transactions) {
            totalVolume += tx.getAmount(); // standardized currency

            if (tx.getType() == FinancialTransaction.TransactionType.CRYPTO_PURCHASE ||
                    tx.getType() == FinancialTransaction.TransactionType.CRYPTO_SALE) {
                cryptoCount++;
            }

            // Check destination country risk
            String destRisk = CountryRiskUtil.getResidenceRisk(tx.getDestinationCountry());
            if ("HIGH".equals(destRisk))
                score += 10;
        }

        // Volume thresholds
        if (totalVolume > 100000)
            score += 40;
        else if (totalVolume > 10000)
            score += 20;

        // Crypto activity
        if (cryptoCount > 0)
            score += 50;

        return score;
    }

    private RiskLevel classifyRisk(int totalScore) {
        if (totalScore >= 71)
            return RiskLevel.HIGH;
        if (totalScore >= 31)
            return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public record RiskScoreResult(
            int totalScore,
            RiskLevel riskLevel,
            int customerScore,
            int geoScore,
            int productScore,
            int transactionScore) {
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
