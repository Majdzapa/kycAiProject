package com.kyc.ai.service;

import com.kyc.ai.agent.RiskAgent;
import com.kyc.ai.entity.Customer;
import com.kyc.ai.entity.FinancialTransaction;
import com.kyc.ai.entity.Product;
import com.kyc.ai.repository.CustomerRepository;
import com.kyc.ai.repository.FinancialTransactionRepository;
import com.kyc.ai.repository.ProductRepository;
import com.kyc.ai.util.CountryRiskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI-Powered Risk Scoring Service
 * 
 * This service combines:
 * 1. Rule-based scoring (regulatory compliance baseline)
 * 2. AI-powered contextual analysis via RAG
 * 3. Dynamic learning from regulatory documents
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIPoweredRiskScoringService {

    private final RagService ragService;
    private final RiskAgent riskAgent;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final PepScreeningService pepScreeningService;
    private final SanctionsScreeningService sanctionsScreeningService;
    private final AdverseMediaService adverseMediaService;

    /**
     * Calculate AI-enhanced risk score
     */
    public AIRiskScoreResult calculateAIPoweredRiskScore(String customerId) {
        log.info("Starting AI-powered risk assessment for customer: {}", customerId);

        try {
            // 1. Gather comprehensive customer data
            CustomerRiskProfile profile = buildCustomerRiskProfile(customerId);

            // 2. Calculate baseline rule-based score (regulatory compliance)
            BaselineRiskScore baselineScore = calculateBaselineScore(profile);

            // 3. Retrieve relevant regulatory context via RAG
            String regulatoryContext = retrieveRegulatoryContext(profile);

            // 4. Get AI-powered risk assessment with regulatory context
            RiskAgent.RiskAssessmentResult aiAssessment = performAIRiskAssessment(
                    profile,
                    regulatoryContext);

            // 5. Combine baseline and AI assessment
            CombinedRiskScore finalScore = combineScores(
                    baselineScore,
                    aiAssessment,
                    profile);

            // 6. Generate actionable recommendations
            List<String> recommendations = generateRecommendations(
                    finalScore,
                    aiAssessment,
                    regulatoryContext);

            log.info("AI-powered risk assessment completed for {}: Score={}, Level={}",
                    customerId, finalScore.totalScore(), finalScore.riskLevel());

            return new AIRiskScoreResult(
                    finalScore.totalScore(),
                    finalScore.riskLevel(),
                    finalScore.dueDiligenceLevel(),
                    finalScore.monitoringFrequency(),
                    baselineScore,
                    aiAssessment,
                    recommendations,
                    regulatoryContext,
                    LocalDateTime.now());

        } catch (Exception e) {
            log.error("AI-powered risk assessment failed for customer: {}", customerId, e);
            // Fallback to baseline scoring
            return fallbackToBaselineScoring(customerId);
        }
    }

    /**
     * Build comprehensive customer risk profile
     */
    private CustomerRiskProfile buildCustomerRiskProfile(String customerId) {
        Customer customer = customerRepository.findByUserId(UUID.fromString(customerId))
                .or(() -> customerRepository.findById(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        String actualCustomerId = customer.getId();


        // Get external screening results
        var pepResult = pepScreeningService.getLatestScreening(actualCustomerId);
        var sanctionsResult = sanctionsScreeningService.getLatestScreening(actualCustomerId);
        var adverseMedia = adverseMediaService.getLatestScreening(actualCustomerId);

        // Get financial data
        List<Product> products = productRepository.findByCustomerId(actualCustomerId);
        List<FinancialTransaction> recentTransactions = transactionRepository
                .findRecentTransactions(actualCustomerId, LocalDateTime.now().minusMonths(3));

        // Calculate transaction metrics
        TransactionMetrics txMetrics = calculateTransactionMetrics(recentTransactions);

        return new CustomerRiskProfile(
                actualCustomerId,
                customer.getFullName(),
                customer.getNationality(),
                customer.getResidenceCountry(),
                customer.getOccupation(),
                customer.getIndustrySector(),
                customer.getIncomeRange(),
                customer.getSourceOfWealth(),
                customer.getEntityType(),
                customer.getNetWorth(),
                pepResult,
                sanctionsResult,
                adverseMedia,
                products,
                txMetrics,
                customer.getAccountAge(),
                customer.getExpectedMonthlyVolume());
    }

    /**
     * Calculate rule-based baseline score
     */
    private BaselineRiskScore calculateBaselineScore(CustomerRiskProfile profile) {
        int customerScore = 0;
        int geoScore = 0;
        int productScore = 0;
        int transactionScore = 0;

        // Customer Risk (0-30)
        if (profile.pepResult() != null && profile.pepResult().isPep()) {
            customerScore += mapPepLevelToScore(profile.pepResult().pepLevel());
        }
        if (profile.sanctionsResult() != null && profile.sanctionsResult().hasMatch()) {
            customerScore += 10; // Max sanctions impact
        }
        if (profile.adverseMedia() != null) {
            int mediaCount = profile.adverseMedia().size();
            customerScore += Math.min(8, mediaCount >= 5 ? 8 : mediaCount >= 3 ? 6 : 3);
        }
        customerScore += getOccupationRisk(profile.occupation(), profile.industrySector());
        customerScore = Math.min(30, customerScore);

        // Geographic Risk (0-25)
        geoScore += getNationalityRiskScore(profile.nationality());
        geoScore += getResidenceRiskScore(profile.residenceCountry());
        geoScore += getTransactionGeographyScore(profile.txMetrics());
        geoScore = Math.min(25, geoScore);

        // Product Risk (0-20)
        productScore = calculateProductRiskScore(profile.products());
        productScore = Math.min(20, productScore);

        // Transaction Risk (0-25)
        transactionScore += getVolumeRisk(
                profile.txMetrics().totalVolume(),
                profile.expectedMonthlyVolume());
        transactionScore += getPatternRisk(profile.txMetrics());
        transactionScore += getPaymentMethodRisk(profile.txMetrics());
        transactionScore = Math.min(25, transactionScore);

        int totalBaseline = customerScore + geoScore + productScore + transactionScore;

        return new BaselineRiskScore(
                totalBaseline,
                customerScore,
                geoScore,
                productScore,
                transactionScore);
    }

    /**
     * Retrieve relevant regulatory context
     */
    private String retrieveRegulatoryContext(CustomerRiskProfile profile) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("KYC AML risk assessment ");

        if (profile.pepResult() != null && profile.pepResult().isPep()) {
            queryBuilder.append("politically exposed person PEP ");
        }
        if (profile.nationality() != null) {
            queryBuilder.append(profile.nationality()).append(" ");
        }
        if (profile.occupation() != null) {
            queryBuilder.append(profile.occupation()).append(" ");
        }
        if (profile.txMetrics().hasCryptoActivity()) {
            queryBuilder.append("cryptocurrency virtual assets ");
        }
        if (profile.txMetrics().hasCashActivity()) {
            queryBuilder.append("cash intensive ");
        }

        return ragService.retrieveContextForChatbot(queryBuilder.toString().trim());
    }

    /**
     * Perform AI risk assessment
     */
    private RiskAgent.RiskAssessmentResult performAIRiskAssessment(
            CustomerRiskProfile profile,
            String regulatoryContext) {

        String unusualPatterns = buildUnusualPatternsDescription(profile);

        if (regulatoryContext != null && !regulatoryContext.isEmpty()) {
            unusualPatterns = "REGULATORY CONTEXT:\n" + regulatoryContext +
                    "\n\nCUSTOMER PATTERNS:\n" + unusualPatterns;
        }

        return riskAgent.assessRisk(
                profile.customerId(),
                profile.nationality() != null ? profile.nationality() : "UNKNOWN",
                profile.residenceCountry() != null ? profile.residenceCountry() : "UNKNOWN",
                profile.occupation() != null ? profile.occupation() : "NOT_SPECIFIED",
                profile.industrySector() != null ? profile.industrySector() : "NOT_SPECIFIED",
                profile.incomeRange() != null ? profile.incomeRange() : "NOT_SPECIFIED",
                profile.sourceOfWealth() != null ? profile.sourceOfWealth() : "NOT_SPECIFIED",
                profile.pepResult() != null && profile.pepResult().isPep(),
                profile.pepResult() != null ? profile.pepResult().pepLevel() : null,
                profile.adverseMedia() != null ? profile.adverseMedia().size() : 0,
                profile.sanctionsResult() != null && profile.sanctionsResult().hasMatch(),
                false, // previousSar placeholder
                CountryRiskUtil.getNationalityRisk(profile.nationality()),
                CountryRiskUtil.getResidenceRisk(profile.residenceCountry()),
                CountryRiskUtil.getFatfStatus(profile.residenceCountry()),
                profile.entityType() != null ? profile.entityType() : null,
                profile.accountAge() != null ? profile.accountAge() : 0,
                "CORPORATION".equals(profile.entityType()),
                profile.txMetrics().hasCashActivity(),
                profile.accountAge() != null ? profile.accountAge() : 0,
                String.format("%.2f", profile.txMetrics().totalVolume()),
                List.of(unusualPatterns));
    }

    /**
     * Combine baseline and AI scores
     */
    private CombinedRiskScore combineScores(
            BaselineRiskScore baseline,
            RiskAgent.RiskAssessmentResult aiResult,
            CustomerRiskProfile profile) {

        int adjustedScore = baseline.totalScore();
        int aiAdjustment = calculateAIAdjustment(baseline, aiResult, profile);
        adjustedScore += aiAdjustment;

        adjustedScore = Math.max(baseline.totalScore() - 10, adjustedScore);
        adjustedScore = Math.max(0, Math.min(100, adjustedScore));

        RiskLevel finalLevel = classifyRiskLevel(adjustedScore);
        DueDiligenceLevel ddLevel = determineDueDiligence(adjustedScore, aiResult);
        MonitoringFrequency monitoring = determineMonitoring(adjustedScore, finalLevel);

        return new CombinedRiskScore(
                adjustedScore,
                finalLevel,
                ddLevel,
                monitoring,
                aiAdjustment,
                aiResult.riskLevel().name(),
                baseline.totalScore());
    }

    private int calculateAIAdjustment(
            BaselineRiskScore baseline,
            RiskAgent.RiskAssessmentResult aiResult,
            CustomerRiskProfile profile) {

        int adjustment = 0;

        if (aiResult.riskFactors() != null) {
            long highSeverityCount = aiResult.riskFactors().stream()
                    .filter(f -> f.severity() == RiskAgent.Severity.HIGH)
                    .count();
            adjustment += Math.min(10, (int) highSeverityCount * 2);
        }

        if (aiResult.mitigatingFactors() != null) {
            adjustment -= Math.min(5, aiResult.mitigatingFactors().size());
        }

        if (aiResult.complianceRequirements() != null && aiResult.complianceRequirements().sarConsideration()) {
            adjustment += 10;
        }

        if (aiResult.complianceRequirements() != null && aiResult.complianceRequirements().eddRequired()
                && baseline.totalScore() < 61) {
            adjustment += 5;
        }

        return adjustment;
    }

    private List<String> generateRecommendations(
            CombinedRiskScore finalScore,
            RiskAgent.RiskAssessmentResult aiResult,
            String regulatoryContext) {

        List<String> recommendations = new ArrayList<>();

        if (finalScore.riskLevel() == RiskLevel.CRITICAL) {
            recommendations.add("☠️ CRITICAL RISK: Immediate senior management approval required");
            recommendations.add("☠️ Consider SAR/STR filing - review within 24 hours");
        } else if (finalScore.riskLevel() == RiskLevel.HIGH) {
            recommendations.add("⚡ HIGH RISK: Compliance officer review required within 48 hours");
            recommendations.add("⚡ Enhanced due diligence (EDD) mandatory");
        }

        if (aiResult.recommendedActions() != null) {
            aiResult.recommendedActions().forEach(action -> recommendations.add("🤖 AI Insight: " + action));
        }

        recommendations.add(String.format("📊 Monitoring: %s reviews required", finalScore.monitoringFrequency()));

        return recommendations;
    }

    private String buildUnusualPatternsDescription(CustomerRiskProfile profile) {
        StringBuilder patterns = new StringBuilder();
        TransactionMetrics metrics = profile.txMetrics();

        if (metrics.hasStructuringPattern())
            patterns.append("Structuring detected. ");
        if (metrics.hasRapidMovement())
            patterns.append("Rapid movement of funds. ");
        if (metrics.hasCryptoActivity())
            patterns.append("Crypto activity detected. ");
        if (metrics.hasCashActivity())
            patterns.append("High cash intensity. ");

        return patterns.length() == 0 ? "No significant unusual patterns." : patterns.toString().trim();
    }

    private AIRiskScoreResult fallbackToBaselineScoring(String customerId) {
        log.warn("Falling back to baseline scoring for customer: {}", customerId);
        try {
            CustomerRiskProfile profile = buildCustomerRiskProfile(customerId);
            BaselineRiskScore baseline = calculateBaselineScore(profile);
            RiskLevel level = classifyRiskLevel(baseline.totalScore());

            return new AIRiskScoreResult(
                    baseline.totalScore(),
                    level,
                    DueDiligenceLevel.STANDARD,
                    MonitoringFrequency.ANNUAL,
                    baseline,
                    null,
                    List.of("AI assessment unavailable - using rule-based scoring only"),
                    "Context unavailable",
                    LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("Risk assessment completely failed", e);
        }
    }

    private TransactionMetrics calculateTransactionMetrics(List<FinancialTransaction> transactions) {
        if (transactions == null || transactions.isEmpty())
            return TransactionMetrics.empty();

        double totalVolume = transactions.stream().mapToDouble(FinancialTransaction::getAmount).sum();
        long cryptoCount = transactions.stream().filter(tx -> tx.getType().name().contains("CRYPTO")).count();
        long cashCount = transactions.stream().filter(tx -> tx.getType().name().contains("CASH")).count();

        return new TransactionMetrics(
                totalVolume,
                transactions.size(),
                false, // structuring placeholder
                false, // rapid movement placeholder
                false, // unusual hours placeholder
                0,
                (int) cryptoCount,
                (int) cashCount,
                0.0,
                cryptoCount > 0,
                cashCount > 0);
    }

    private int mapPepLevelToScore(String pepLevel) {
        if (pepLevel == null)
            return 8;
        return switch (pepLevel.toUpperCase()) {
            case "FOREIGN_SENIOR_OFFICIAL" -> 12;
            case "DOMESTIC_SENIOR_OFFICIAL" -> 10;
            default -> 8;
        };
    }

    private int getOccupationRisk(String occupation, String industry) {
        if (occupation == null)
            return 2;
        String occ = occupation.toUpperCase();
        if (occ.contains("CRYPTO") || occ.contains("ARMS") || occ.contains("GAMBLING"))
            return 10;
        return 2;
    }

    private int getNationalityRiskScore(String nationality) {
        String risk = CountryRiskUtil.getNationalityRisk(nationality);
        return "CRITICAL".equals(risk) ? 15 : "HIGH".equals(risk) ? 10 : 0;
    }

    private int getResidenceRiskScore(String residence) {
        String risk = CountryRiskUtil.getResidenceRisk(residence);
        return "HIGH".equals(risk) ? 10 : "MEDIUM".equals(risk) ? 5 : 0;
    }

    private int getTransactionGeographyScore(TransactionMetrics metrics) {
        return 0; // placeholder
    }

    private int calculateProductRiskScore(List<Product> products) {
        if (products == null || products.isEmpty())
            return 0;
        return products.stream()
                .mapToInt(p -> "HIGH".equals(p.getBaseRiskLevel().name()) ? 15
                        : "MEDIUM".equals(p.getBaseRiskLevel().name()) ? 8 : 2)
                .max().orElse(0);
    }

    private int getVolumeRisk(double actual, Double expected) {
        if (expected == null || expected <= 0)
            return actual > 100000 ? 8 : 0;
        return (actual / expected) >= 2 ? 8 : 0;
    }

    private int getPatternRisk(TransactionMetrics metrics) {
        return 0;
    }

    private int getPaymentMethodRisk(TransactionMetrics metrics) {
        return (metrics.hasCryptoActivity() ? 6 : 0) + (metrics.hasCashActivity() ? 4 : 0);
    }

    private RiskLevel classifyRiskLevel(int score) {
        if (score >= 91)
            return RiskLevel.CRITICAL;
        if (score >= 76)
            return RiskLevel.HIGH;
        if (score >= 61)
            return RiskLevel.MEDIUM_HIGH;
        if (score >= 41)
            return RiskLevel.MEDIUM;
        if (score >= 21)
            return RiskLevel.MEDIUM_LOW;
        return RiskLevel.LOW;
    }

    private DueDiligenceLevel determineDueDiligence(int score, RiskAgent.RiskAssessmentResult aiResult) {
        if (aiResult != null && aiResult.complianceRequirements() != null
                && aiResult.complianceRequirements().eddRequired())
            return DueDiligenceLevel.ENHANCED;
        return score >= 61 ? DueDiligenceLevel.ENHANCED : DueDiligenceLevel.STANDARD;
    }

    private MonitoringFrequency determineMonitoring(int score, RiskLevel level) {
        if (score >= 76)
            return MonitoringFrequency.CONTINUOUS;
        if (score >= 61)
            return MonitoringFrequency.MONTHLY;
        return MonitoringFrequency.ANNUAL;
    }

    // Records and Enums
    public record AIRiskScoreResult(
            int totalScore,
            RiskLevel riskLevel,
            DueDiligenceLevel dueDiligenceLevel,
            MonitoringFrequency monitoringFrequency,
            BaselineRiskScore baselineScore,
            RiskAgent.RiskAssessmentResult aiAssessment,
            List<String> recommendations,
            String regulatoryContext,
            LocalDateTime assessmentDate) {
    }

    public record BaselineRiskScore(
            int totalScore,
            int customerScore,
            int geoScore,
            int productScore,
            int transactionScore) {
    }

    public record CombinedRiskScore(
            int totalScore,
            RiskLevel riskLevel,
            DueDiligenceLevel dueDiligenceLevel,
            MonitoringFrequency monitoringFrequency,
            int aiAdjustment,
            String aiRiskLevel,
            int baselineScore) {
    }

    public record CustomerRiskProfile(
            String customerId,
            String fullName,
            String nationality,
            String residenceCountry,
            String occupation,
            String industrySector,
            String incomeRange,
            String sourceOfWealth,
            String entityType,
            Double netWorth,
            PepScreeningResult pepResult,
            SanctionsScreeningResult sanctionsResult,
            List<AdverseMediaResult> adverseMedia,
            List<Product> products,
            TransactionMetrics txMetrics,
            Integer accountAge,
            Double expectedMonthlyVolume) {
    }

    public record TransactionMetrics(
            double totalVolume,
            int transactionCount,
            boolean hasStructuringPattern,
            boolean hasRapidMovement,
            boolean hasUnusualHours,
            int highRiskDestinationCount,
            int cryptoTransactionCount,
            int cashTransactionCount,
            double roundAmountPercentage,
            boolean hasCryptoActivity,
            boolean hasCashActivity) {
        public static TransactionMetrics empty() {
            return new TransactionMetrics(0, 0, false, false, false, 0, 0, 0, 0, false, false);
        }

        public double cashPercentage() {
            return transactionCount > 0 ? (double) cashTransactionCount / transactionCount : 0;
        }
    }

    public enum RiskLevel {
        LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH, CRITICAL
    }

    public enum DueDiligenceLevel {
        SIMPLIFIED, STANDARD, ENHANCED
    }

    public enum MonitoringFrequency {
        ANNUAL, SEMI_ANNUAL, QUARTERLY, MONTHLY, CONTINUOUS, REAL_TIME
    }

    public record PepScreeningResult(boolean isPep, String pepLevel, String position, String organization) {
    }

    public record SanctionsScreeningResult(boolean hasMatch, String listName, double matchScore) {
    }

    public record AdverseMediaResult(String headline, String category, String severity, LocalDateTime date) {
    }
}
